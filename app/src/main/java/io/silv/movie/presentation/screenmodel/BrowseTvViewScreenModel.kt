package io.silv.movie.presentation.screenmodel

import androidx.compose.runtime.Stable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.movie.data.content.movie.local.CreditRepository
import io.silv.movie.data.content.movie.local.ShowRepository
import io.silv.movie.data.content.movie.local.TrailerRepository
import io.silv.movie.data.content.movie.local.awaitUpdateFromSource
import io.silv.movie.data.content.movie.local.networkToLocalCredit
import io.silv.movie.data.content.movie.local.networkToLocalShow
import io.silv.movie.data.content.movie.network.SourceCreditsRepository
import io.silv.movie.data.content.movie.model.toDomain
import io.silv.movie.data.content.movie.network.SourceTrailerRepository
import io.silv.movie.data.content.movie.model.Trailer
import io.silv.movie.data.content.movie.model.TVShow
import io.silv.movie.data.content.movie.network.SourceShowRepository
import io.silv.movie.data.content.movie.network.networkToLocalTrailer
import io.silv.movie.presentation.covers.cache.TVShowCoverCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import timber.log.Timber

class TVViewScreenModel(
    private val trailerSource: SourceTrailerRepository,
    private val trailerRepo: TrailerRepository,
    private val creditsRepo: CreditRepository,
    private val creditsSource: SourceCreditsRepository,
    private val showRepo: ShowRepository,
    private val showSource: SourceShowRepository,
    private val showCoverCache: TVShowCoverCache,
    private val showId: Long
): StateScreenModel<ShowDetailsState>(ShowDetailsState.Loading) {

    private fun MutableStateFlow<ShowDetailsState>.updateSuccess(
        function: (ShowDetailsState.Success) -> ShowDetailsState.Success
    ) {
        update {
            when (it) {
                is ShowDetailsState.Success -> function(it)
                else -> it
            }
        }
    }

    init {
        screenModelScope.launch {
            try {
                val show = showRepo.getShowById(showId)

                when {
                    show == null  -> {
                        val sshow = showSource.getShow(showId)

                        mutableState.value = if (sshow != null) {
                            ShowDetailsState.Success(
                                show = showRepo.networkToLocalShow(sshow.toDomain())
                            )
                        } else {
                            ShowDetailsState.Error
                        }
                    }
                    show.needsInit -> {
                        mutableState.value =
                            ShowDetailsState.Success(show = show)
                        refreshShowInfo()
                    }
                    else -> {
                        mutableState.value =
                            ShowDetailsState.Success(show = show)
                    }
                }
                refreshShowCredits()
            }catch (e: Exception) {
                mutableState.value = ShowDetailsState.Error
            }
        }

        screenModelScope.launch {
            state.map { it.success?.show?.id }
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { showId ->
                    val trailers = runCatching {
                        trailerRepo.getByShowId(showId)
                    }

                    if (trailers.isSuccess) {
                        mutableState.updateSuccess { state ->
                            state.copy(trailers = trailers.getOrDefault(emptyList()))
                        }
                    } else {
                       refreshShowTrailers()
                    }
                }
        }

        showRepo.observeShowByIdOrNull(showId).filterNotNull().onEach { new ->
            mutableState.updateSuccess {
                it.copy(show = new)
            }
        }
            .launchIn(screenModelScope)
    }

    val credits = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = { creditsRepo.showCreditsPagingSource(showId) },
    ).flow
        .cachedIn(screenModelScope)
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PagingData.empty()
        )


    private suspend fun refreshShowCredits() {
        runCatching { creditsSource.awaitShow(showId) }
            .onSuccess { credits ->
                val show =  state.value.success?.show
                Timber.d(credits.toString())
                for (sCredit in credits) {
                    creditsRepo.networkToLocalCredit(
                        sCredit.toDomain().copy(posterPath = show?.posterUrl, title = show?.title.orEmpty()),
                        showId,
                        false
                    )
                }
            }
            .onFailure { Timber.e(it) }
    }



    private suspend fun refreshShowTrailers() {

        val trailers = runCatching { trailerSource.awaitShow(showId) }.getOrDefault(emptyList())
            .map {
                trailerRepo.networkToLocalTrailer(
                    it.toDomain(), showId, false
                )
            }

        mutableState.updateSuccess {state ->
            state.copy(
                trailers = trailers
            )
        }
    }

    private suspend fun refreshShowInfo() {

        val sshow = runCatching{ showSource.getShow(showId) }.getOrNull()
        val show = state.value.success?.show

        if (sshow != null && show != null) {
            showRepo.awaitUpdateFromSource(show, sshow, showCoverCache)
        }
    }

    fun updateDialog(dialog: Dialog?) {
        screenModelScope.launch {
            mutableState.updateSuccess {state ->
                state.copy(dialog = dialog)
            }
        }
    }


    fun refresh() {
        screenModelScope.launch {

            mutableState.updateSuccess { it.copy(refreshing = true) }

            listOf(
                launch { refreshShowInfo() },
                launch { refreshShowTrailers() },
                launch { refreshShowCredits() }
            )
                .joinAll()

            mutableState.updateSuccess { it.copy(refreshing = false) }
        }
    }

    @Stable
    sealed interface Dialog {

        @Stable
        data object FullCover: Dialog

        @Stable
        data object Comments: Dialog
    }
}


sealed class ShowDetailsState {

    @Stable
    data object Error: ShowDetailsState()

    @Stable
    data object Loading: ShowDetailsState()

    @Stable
    data class Success(
        val show: TVShow,
        val trailers: List<Trailer> = listOf(),
        val refreshing: Boolean = false,
        val dialog: TVViewScreenModel.Dialog? = null,
    ): ShowDetailsState()

    val success
        get() = this as? Success
}
package io.silv.movie.presentation.screenmodel

import androidx.compose.runtime.Stable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.movie.data.content.credits.CreditRepository
import io.silv.movie.data.content.credits.GetRemoteCredits
import io.silv.movie.data.content.credits.NetworkToLocalCredit
import io.silv.movie.data.content.credits.toDomain
import io.silv.movie.data.content.trailers.GetRemoteTrailers
import io.silv.movie.data.content.trailers.GetTVShowTrailers
import io.silv.movie.data.content.trailers.NetworkToLocalTrailer
import io.silv.movie.data.content.trailers.Trailer
import io.silv.movie.data.content.trailers.toDomain
import io.silv.movie.data.content.tv.interactor.GetRemoteTVShows
import io.silv.movie.data.content.tv.interactor.GetShow
import io.silv.movie.data.content.tv.interactor.NetworkToLocalTVShow
import io.silv.movie.data.content.tv.interactor.UpdateShow
import io.silv.movie.data.content.tv.model.TVShow
import io.silv.movie.data.content.tv.model.toDomain
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
    private val getTVShowTrailers: GetTVShowTrailers,
    private val getRemoteTrailers: GetRemoteTrailers,
    private val networkToLocalShow: NetworkToLocalTVShow,
    private val networkToLocalTrailer: NetworkToLocalTrailer,
    private val getRemoteShow: GetRemoteTVShows,
    private val creditRepository: CreditRepository,
    private val getRemoteCredits: GetRemoteCredits,
    private val networkToLocalCredit: NetworkToLocalCredit,
    private val getShow: GetShow,
    private val updateShow: UpdateShow,
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
                val show = getShow.await(id = showId)

                when {
                    show == null  -> {
                        val sshow = getRemoteShow.awaitOne(showId)

                        mutableState.value = if (sshow != null) {
                            ShowDetailsState.Success(
                                show = networkToLocalShow.await(sshow.toDomain())
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

                    val trailers = runCatching { getTVShowTrailers.await(showId) }.getOrDefault(emptyList())

                    if (trailers.isEmpty()) {
                        refreshShowTrailers()
                    } else {
                        mutableState.updateSuccess { state ->
                            state.copy(trailers = trailers)
                        }
                    }
                }
        }

        getShow.subscribeOrNull(showId).filterNotNull().onEach { new ->
            mutableState.updateSuccess {
                it.copy(show = new)
            }
        }
            .launchIn(screenModelScope)
    }

    val credits = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = { creditRepository.showCreditsPagingSource(showId) },
    ).flow
        .cachedIn(screenModelScope)
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PagingData.empty()
        )


    private suspend fun refreshShowCredits() {
        runCatching { getRemoteCredits.awaitShow(showId) }
            .onSuccess { credits ->
                val show =  state.value.success?.show
                Timber.d(credits.toString())
                for (sCredit in credits) {
                    networkToLocalCredit.await(
                        sCredit.toDomain().copy(posterPath = show?.posterUrl, title = show?.title.orEmpty()),
                        showId,
                        false
                    )
                }
            }
            .onFailure { Timber.e(it) }
    }



    private suspend fun refreshShowTrailers() {

        val trailers = runCatching { getRemoteTrailers.awaitShow(showId) }.getOrDefault(emptyList())
            .map {
                networkToLocalTrailer.await(
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

        val sshow = runCatching{ getRemoteShow.awaitOne(showId) }.getOrNull()
        val show = state.value.success?.show

        if (sshow != null && show != null) {
            updateShow.awaitUpdateFromSource(show, sshow, showCoverCache)
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
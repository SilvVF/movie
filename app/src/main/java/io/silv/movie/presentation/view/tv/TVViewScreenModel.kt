package io.silv.movie.presentation.view.tv

import androidx.compose.runtime.Stable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.movie.data.cache.TVShowCoverCache
import io.silv.movie.data.credits.CreditRepository
import io.silv.movie.data.credits.GetRemoteCredits
import io.silv.movie.data.credits.NetworkToLocalCredit
import io.silv.movie.data.credits.toDomain
import io.silv.movie.data.trailers.GetRemoteTrailers
import io.silv.movie.data.trailers.GetTVShowTrailers
import io.silv.movie.data.trailers.NetworkToLocalTrailer
import io.silv.movie.data.trailers.Trailer
import io.silv.movie.data.trailers.toDomain
import io.silv.movie.data.tv.interactor.GetRemoteTVShows
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.tv.interactor.NetworkToLocalTVShow
import io.silv.movie.data.tv.interactor.UpdateShow
import io.silv.movie.data.tv.model.TVShow
import io.silv.movie.data.tv.model.toDomain
import io.silv.movie.data.tv.model.toShowUpdate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
                        refreshShowCredits()
                    }
                    else -> {
                        mutableState.value =
                            ShowDetailsState.Success(show = show)
                    }
                }
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
                            state.copy(
                                trailers = trailers.toImmutableList()
                            )
                        }
                    }
                }
        }

        getShow.subscribe(showId).onEach { new ->
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
                Timber.d(credits.toString())
                for (sCredit in credits) {
                    networkToLocalCredit.await(sCredit.toDomain(), showId, false)
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
                trailers = trailers.toImmutableList()
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

    fun toggleShowFavorite(tvShow: TVShow) {
        screenModelScope.launch {
            val show = getShow.await(tvShow.id) ?: return@launch

            val new = show.copy(favorite = !show.favorite)

            if(!new.favorite && !new.inList) {
                showCoverCache.deleteFromCache(show)
            }
            updateShow.await(new.toShowUpdate())
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
        val trailers: ImmutableList<Trailer> = persistentListOf(),
        val refreshing: Boolean = false,
        val dialog: TVViewScreenModel.Dialog? = null,
    ): ShowDetailsState()

    val success
        get() = this as? Success
}
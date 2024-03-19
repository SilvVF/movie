package io.silv.movie.presentation.view.tv

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.movie.data.cache.TVShowCoverCache
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class TVViewScreenModel(
    private val getTVShowTrailers: GetTVShowTrailers,
    private val getRemoteTrailers: GetRemoteTrailers,
    private val networkToLocalShow: NetworkToLocalTVShow,
    private val networkToLocalTrailer: NetworkToLocalTrailer,
    private val getRemoteShow: GetRemoteTVShows,
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
            if (mutableState.value is ShowDetailsState.Success) { return@launch }

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
        }

        screenModelScope.launch {
            state.map { it.success?.show?.id }
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { showId ->

                    val trailers = getTVShowTrailers.await(showId)

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


    private suspend fun refreshShowTrailers() {

        val trailers = getRemoteTrailers.awaitShow(showId)
            .map {
                networkToLocalTrailer.await(
                    it.toDomain()
                        .copy(
                            contentId = showId,
                            isMovie = false
                        )
                )
            }

        mutableState.updateSuccess {state ->
            state.copy(
                trailers = trailers.toImmutableList()
            )
        }
    }

    private suspend fun refreshShowInfo() {

        val sshow = getRemoteShow.awaitOne(showId)
        val show = state.value.success?.show

        if (sshow != null && show != null) {
            updateShow.awaitUpdateFromSource(show, sshow, showCoverCache)
        }
    }

    fun toggleShowFavorite(show: TVShow) {
        screenModelScope.launch {
            val update = show.copy(favorite = !show.favorite).toShowUpdate()

            updateShow.await(update)
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
                launch { refreshShowTrailers() }
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
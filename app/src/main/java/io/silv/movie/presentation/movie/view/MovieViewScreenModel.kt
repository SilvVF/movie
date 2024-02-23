package io.silv.movie.presentation.movie.view

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.data.movie.interactor.GetMovie
import io.silv.data.movie.model.Movie
import io.silv.data.trailers.GetMovieTrailers
import io.silv.data.trailers.GetRemoteTrailers
import io.silv.data.trailers.NetworkToLocalTrailer
import io.silv.data.trailers.Trailer
import io.silv.data.trailers.toDomain
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
import kotlinx.coroutines.launch

class MovieViewScreenModel(
    private val getMovieTrailers: GetMovieTrailers,
    private val getRemoteTrailers: GetRemoteTrailers,
    private val networkToLocalTrailer: NetworkToLocalTrailer,
    private val getMovie: GetMovie,
    private val movieId: Long
): StateScreenModel<MovieDetailsState>(MovieDetailsState.Loading) {

    val success: MovieDetailsState.Success?
        get() = mutableState.value as? MovieDetailsState.Success

    private fun MutableStateFlow<MovieDetailsState>.updateSuccess(
        function: (MovieDetailsState.Success) -> MovieDetailsState.Success
    ) {
        update {
            when (it) {
                is MovieDetailsState.Success -> function(it)
                else -> it
            }
        }
    }

    init {

        screenModelScope.launch {
            if (mutableState.value is MovieDetailsState.Success) { return@launch }

            val movie = getMovie.await(id = movieId)

            if (movie == null) {
                mutableState.value = MovieDetailsState.Error
                return@launch
            } else {
                mutableState.value = MovieDetailsState.Success(movie = movie)
            }
        }

        screenModelScope.launch {
            state.map { it.success?.movie?.id }
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { movieId ->

                    val trailers = getMovieTrailers.await(movieId)
                        .ifEmpty {
                            getRemoteTrailers.await(movieId).map {
                                networkToLocalTrailer.await(it.toDomain().copy(movieId = movieId))
                            }
                        }

                    mutableState.updateSuccess {state ->
                        state.copy(
                            trailers = trailers.toImmutableList()
                        )
                    }
                }
        }

        getMovie.subscribe(movieId).onEach { new ->
            mutableState.updateSuccess {
                it.copy(movie = new)
            }
        }
            .launchIn(screenModelScope)
    }
}

sealed class MovieDetailsState {

    @Stable
    data object Error: MovieDetailsState()

    @Stable
    data object Loading: MovieDetailsState()

    @Stable
    data class Success(
        val movie: Movie,
        val trailers: ImmutableList<Trailer> = persistentListOf()
    ): MovieDetailsState()

    val success
        get() = this as? Success
}
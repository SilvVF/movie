package io.silv.movie.presentation.view.movie

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.movie.interactor.GetRemoteMovie
import io.silv.movie.data.movie.interactor.NetworkToLocalMovie
import io.silv.movie.data.movie.interactor.UpdateMovie
import io.silv.movie.data.movie.model.Movie
import io.silv.movie.data.movie.model.toDomain
import io.silv.movie.data.movie.model.toMovieUpdate
import io.silv.movie.data.trailers.GetMovieTrailers
import io.silv.movie.data.trailers.GetRemoteTrailers
import io.silv.movie.data.trailers.NetworkToLocalTrailer
import io.silv.movie.data.trailers.Trailer
import io.silv.movie.data.trailers.toDomain
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

class MovieViewScreenModel(
    private val getMovieTrailers: GetMovieTrailers,
    private val getRemoteTrailers: GetRemoteTrailers,
    private val networkToLocalMovie: NetworkToLocalMovie,
    private val networkToLocalTrailer: NetworkToLocalTrailer,
    private val getRemoteMovie: GetRemoteMovie,
    private val getMovie: GetMovie,
    private val updateMovie: UpdateMovie,
    private val movieId: Long
): StateScreenModel<MovieDetailsState>(MovieDetailsState.Loading) {

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

            if (movie == null || movie.needsInit) {
                val smovie = getRemoteMovie.awaitOne(movieId)
                if (smovie == null) {
                    mutableState.value = MovieDetailsState.Error
                    return@launch
                }
                mutableState.value =
                    MovieDetailsState.Success(movie = networkToLocalMovie.await(smovie.toDomain()))
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

                    if (trailers.isEmpty()) {
                        refreshMovieTrailers()
                    } else {
                        mutableState.updateSuccess { state ->
                            state.copy(
                                trailers = trailers.toImmutableList()
                            )
                        }
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


    private suspend fun refreshMovieTrailers() {

        val trailers = getRemoteTrailers.awaitMovie(movieId)
            .map {
                networkToLocalTrailer.await(
                    it.toDomain().copy(contentId = movieId, isMovie = true)
                )
            }

        mutableState.updateSuccess {state ->
            state.copy(
                trailers = trailers.toImmutableList()
            )
        }
    }

    private suspend fun refreshMovieInfo() {

        val smovie = getRemoteMovie.awaitOne(movieId) ?: return
        val localMovie = networkToLocalMovie.await(smovie.toDomain())

        mutableState.updateSuccess {state ->
            state.copy(
                movie = localMovie
            )
        }
    }

    fun toggleMovieFavorite(movie: Movie) {
        screenModelScope.launch {
            val update = movie.copy(favorite = !movie.favorite).toMovieUpdate()

            updateMovie.await(update)
        }
    }

    fun refresh() {
        screenModelScope.launch {

            mutableState.updateSuccess { it.copy(refreshing = true) }

            listOf(
                launch { refreshMovieInfo() },
                launch { refreshMovieTrailers() }
            )
                .joinAll()

            mutableState.updateSuccess { it.copy(refreshing = false) }
        }
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
        val trailers: ImmutableList<Trailer> = persistentListOf(),
        val refreshing: Boolean = false
    ): MovieDetailsState()

    val success
        get() = this as? Success
}
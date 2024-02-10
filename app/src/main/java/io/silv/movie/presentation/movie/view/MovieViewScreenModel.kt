package io.silv.movie.presentation.movie.view

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.data.movie.interactor.GetMovie
import io.silv.data.movie.interactor.GetMovieDetails
import io.silv.data.movie.model.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MovieViewScreenModel(
    private val getMovieDetails: GetMovieDetails,
    private val getMovie: GetMovie,
    private val movieId: Long
): StateScreenModel<MovieDetailsState>(MovieDetailsState.Loading) {

    val success: MovieDetailsState.Success?
        get() = mutableState.value as? MovieDetailsState.Success

    private fun MutableStateFlow<MovieDetailsState>.updateSuccess(
        function: MovieDetailsState.Success.() -> MovieDetailsState
    ) {
        update {
            (it as? MovieDetailsState.Success)?.apply { function() } ?: it
        }
    }

    init {

        screenModelScope.launch {
            if (mutableState.value is MovieDetailsState.Success) { return@launch }

            val movie = getMovie.await(id = movieId)

            if (movie == null) {
                mutableState.value = MovieDetailsState.Error
                return@launch
            }
        }

        state.map { (it as? MovieDetailsState.Success)?.movie }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach {
                val details = getMovieDetails.await(it.id)

                mutableState.updateSuccess {
                    copy(details = details)
                }
            }
            .launchIn(screenModelScope)

        getMovie.subscribe(movieId).onEach { new ->
            mutableState.updateSuccess {
                copy(movie = new)
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
        val details: String
    ): MovieDetailsState()
}
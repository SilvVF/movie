package io.silv.data.movie.interactor

import io.silv.data.movie.model.Movie
import io.silv.data.movie.model.TVShow
import io.silv.data.movie.repository.MovieRepository
import io.silv.data.movie.repository.ShowRepository
import kotlinx.coroutines.flow.Flow

class GetMovie(
    private val movieRepository: MovieRepository
) {

    suspend fun await(id: Long): Movie? {
        return movieRepository.getMovieById(id)
    }

    fun subscribeOrNull(id: Long): Flow<Movie?> {
        return movieRepository.observeMovieById(id)
    }

    fun subscribe(id: Long): Flow<Movie> {
        return movieRepository.observeMovieById(id)
    }
}

class GetShow(
    private val showRepository: ShowRepository
) {

    suspend fun await(id: Long): TVShow? {
        return showRepository.getShowById(id)
    }

    fun subscribeOrNull(id: Long): Flow<TVShow?> {
        return showRepository.observeShowByIdOrNull(id)
    }

    fun subscribe(id: Long): Flow<TVShow> {
        return showRepository.observeShowById(id)
    }
}
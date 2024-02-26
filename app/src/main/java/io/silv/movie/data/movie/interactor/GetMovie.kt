package io.silv.movie.data.movie.interactor

import io.silv.movie.data.movie.model.Movie
import io.silv.movie.data.movie.repository.MovieRepository
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

package io.silv.movie.data.content.movie.interactor

import io.silv.movie.data.content.movie.model.Movie
import io.silv.movie.data.content.movie.model.MoviePoster
import io.silv.movie.data.content.movie.repository.MovieRepository
import kotlinx.coroutines.flow.Flow

class GetMovie(
    private val movieRepository: MovieRepository
) {

    suspend fun await(id: Long): Movie? {
        return movieRepository.getMovieById(id)
    }

    fun subscribePartial(id: Long): Flow<MoviePoster>  {
        return movieRepository.observeMoviePartialById(id)
    }

    fun subscribePartialOrNull(id: Long): Flow<MoviePoster?>  {
        return movieRepository.observeMoviePartialByIdOrNull(id)
    }

    fun subscribeOrNull(id: Long): Flow<Movie?> {
        return movieRepository.observeMovieByIdOrNull(id)
    }

    fun subscribe(id: Long): Flow<Movie> {
        return movieRepository.observeMovieById(id)
    }
}

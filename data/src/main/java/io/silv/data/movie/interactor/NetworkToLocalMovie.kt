package io.silv.data.movie.interactor

import io.silv.data.movie.model.Movie
import io.silv.data.movie.repository.MovieRepository

class NetworkToLocalMovie(
    private val movieRepository: MovieRepository,
) {

    suspend fun await(movie: Movie): Movie {
        val localMovie = getMovie(movie.id)
        return when {
            localMovie == null -> {
                val id = insertMovie(movie)
                movie.copy(id = id!!)
            }
            !localMovie.favorite ->
                localMovie.copy(
                    title = movie.title,
                    posterUrl = movie.posterUrl ?: localMovie.posterUrl
                )
            else -> localMovie
        }
    }

    private suspend fun getMovie(id: Long): Movie? {
        return movieRepository.getMovieById(id)
    }

    private suspend fun insertMovie(movie: Movie): Long? {
        return movieRepository.insertMovie(movie)
    }
}
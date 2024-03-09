package io.silv.movie.data.movie.interactor

import io.silv.movie.data.movie.model.Movie
import io.silv.movie.data.movie.model.toMovieUpdate
import io.silv.movie.data.movie.repository.MovieRepository
import kotlinx.datetime.Clock

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
                    posterUrl = movie.posterUrl ?: localMovie.posterUrl,
                    status = movie.status ?: localMovie.status,
                    popularity = movie.popularity,
                    voteCount = movie.voteCount,
                    productionCompanies = movie.productionCompanies ?: localMovie.productionCompanies,
                )
            else -> {
                val updated =  localMovie.copy(
                    title = movie.title,
                    posterUrl = movie.posterUrl ?: localMovie.posterUrl,
                    status = movie.status ?: localMovie.status,
                    popularity = movie.popularity,
                    voteCount = movie.voteCount,
                    productionCompanies = movie.productionCompanies ?: localMovie.productionCompanies,
                    posterLastUpdated = Clock.System.now().epochSeconds
                )
                if(movieRepository.updateMovie(updated.toMovieUpdate())) {
                    updated
                } else {
                    localMovie
                }
            }
        }
    }

    private suspend fun getMovie(id: Long): Movie? {
        return movieRepository.getMovieById(id)
    }

    private suspend fun insertMovie(movie: Movie): Long? {
        return movieRepository.insertMovie(movie)
    }
}

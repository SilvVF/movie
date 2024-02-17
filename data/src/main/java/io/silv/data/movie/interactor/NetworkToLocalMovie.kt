package io.silv.data.movie.interactor

import android.util.Log
import io.silv.data.movie.model.Movie
import io.silv.data.movie.model.TVShow
import io.silv.data.movie.repository.MovieRepository
import io.silv.data.movie.repository.ShowRepository

class NetworkToLocalMovie(
    private val movieRepository: MovieRepository,
) {

    suspend fun await(movie: Movie): Movie {
        val localMovie = getMovie(movie.id)
        return when {
            localMovie == null -> {
                val id = insertMovie(movie)
                Log.d("NetworkToLocalMovie", "inserted id: $id")
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

class NetworkToLocalTVShow(
    private val showRepository: ShowRepository,
) {

    suspend fun await(show: TVShow): TVShow {
        val localShow = getTVShow(show.id)
        return when {
            localShow == null -> {
                val id = insertShow(show)
                show.copy(id = id!!)
            }
            !localShow.favorite ->
                localShow.copy(
                    title = show.title,
                    posterUrl = show.posterUrl ?: localShow.posterUrl
                )
            else -> localShow
        }
    }

    private suspend fun getTVShow(id: Long): TVShow? {
        return showRepository.getShowById(id)
    }

    private suspend fun insertShow(show: TVShow): Long? {
        return showRepository.insertShow(show)
    }
}
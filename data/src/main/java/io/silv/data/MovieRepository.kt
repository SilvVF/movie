package io.silv.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.ConcurrentHashMap

interface MovieRepository {
    suspend fun getMovieById(id: Long): Movie?
    fun observeMovieById(id: Long): Flow<Movie>
    suspend fun insertMovie(movie: Movie): Long?
}

class MovieRepositoryImpl: MovieRepository {

    private val movies = ConcurrentHashMap<Long, Movie>()

    override suspend fun getMovieById(id: Long): Movie? {
        return movies[id]
    }

    override fun observeMovieById(id: Long): Flow<Movie> {
        return flowOf(movies[id]!!)
    }

    override suspend fun insertMovie(movie: Movie): Long? {
        movies[movie.id] = movie
        return movie.id
    }
}
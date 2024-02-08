package io.silv.data.movie.repository

import io.silv.core_database.DatabaseHandler
import io.silv.data.movie.MovieMapper
import io.silv.data.movie.model.Movie
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    suspend fun getMovieById(id: Long): Movie?
    fun observeMovieById(id: Long): Flow<Movie>
    suspend fun insertMovie(movie: Movie): Long?
}

class MovieRepositoryImpl(
    private val handler: DatabaseHandler
): MovieRepository {

    override suspend fun getMovieById(id: Long): Movie? {
        return handler.awaitOneOrNull { movieQueries.selectById(id, MovieMapper.mapMovie) }
    }

    override fun observeMovieById(id: Long): Flow<Movie> {
        return handler.subscribeToOne { movieQueries.selectById(id, MovieMapper.mapMovie) }
    }

    override suspend fun insertMovie(movie: Movie): Long? {
       return handler.awaitOneOrNullExecutable(inTransaction = true) {
           movieQueries.insert(movie.id, movie.title, movie.posterUrl, movie.posterLastUpdated, movie.favorite)
           movieQueries.lastInsertRowId()
       }
    }
}
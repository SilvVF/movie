package io.silv.data.movie.repository

import io.silv.core_database.DatabaseHandler
import io.silv.data.movie.MovieMapper
import io.silv.data.movie.ShowMapper
import io.silv.data.movie.model.Movie
import io.silv.data.movie.model.MovieUpdate
import io.silv.data.movie.model.ShowUpdate
import io.silv.data.movie.model.TVShow
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    suspend fun getMovieById(id: Long): Movie?
    fun observeMovieById(id: Long): Flow<Movie>
    fun observeMovieByIdOrNull(id: Long): Flow<Movie?>
    suspend fun insertMovie(movie: Movie): Long?
    suspend fun updateMovie(update: MovieUpdate): Boolean
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

    override fun observeMovieByIdOrNull(id: Long): Flow<Movie?> {
        return handler.subscribeToOneOrNull { movieQueries.selectById(id, MovieMapper.mapMovie) }
    }

    override suspend fun insertMovie(movie: Movie): Long? {
       return handler.awaitOneOrNullExecutable(inTransaction = true) {
           movieQueries.insert(
               movie.id,
               movie.title,
               movie.overview,
               movie.genres.ifEmpty { null },
               movie.genreIds.ifEmpty { null },
               movie.originalLanguage,
               movie.voteCount.toLong(),
               movie.releaseDate,
               movie.posterUrl,
               movie.posterLastUpdated,
               movie.favorite,
               movie.externalUrl,
               movie.popularity
           )
           movieQueries.lastInsertRowId()
       }
    }

    override suspend fun updateMovie(update: MovieUpdate): Boolean {
        return runCatching {
            partialUpdateMovie(update)
        }
            .isSuccess
    }
    private suspend fun partialUpdateMovie(vararg updates: MovieUpdate) {
        return handler.await {
            updates.forEach { update ->
                movieQueries.update(
                    title = update.title,
                    posterUrl = update.posterUrl,
                    posterLastUpdated = update.posterLastUpdated,
                    favorite = update.favorite,
                    externalUrl = update.externalUrl,
                    movieId = update.movieId,
                    overview = update.overview,
                    genreIds = update.genreIds?.joinToString(","),
                    genres = update.genres?.joinToString("<|>"),
                    originalLanguage = update.originalLanguage,
                    voteCount = update.voteCount?.toLong(),
                    releaseDate = update.releaseDate,
                    popularity = update.popularity
                )
            }
        }
    }
}

interface ShowRepository {
    suspend fun getShowById(id: Long): TVShow?
    fun observeShowById(id: Long): Flow<TVShow>
    fun observeShowByIdOrNull(id: Long): Flow<TVShow?>
    suspend fun insertShow(show: TVShow): Long?
    suspend fun updateShow(update: ShowUpdate): Boolean
}

class ShowRepositoryImpl(
    private val handler: DatabaseHandler
): ShowRepository {

    override suspend fun getShowById(id: Long): TVShow? {
        return handler.awaitOneOrNull { showQueries.selectById(id, ShowMapper.mapShow) }
    }

    override fun observeShowById(id: Long): Flow<TVShow> {
        return handler.subscribeToOne { showQueries.selectById(id, ShowMapper.mapShow) }
    }

    override fun observeShowByIdOrNull(id: Long): Flow<TVShow?> {
        return handler.subscribeToOneOrNull { showQueries.selectById(id, ShowMapper.mapShow) }
    }

    override suspend fun insertShow(show: TVShow): Long? {
        return handler.awaitOneOrNullExecutable(inTransaction = true) {
            showQueries.insert(show.id, show.title, show.posterUrl, show.posterLastUpdated, show.favorite, show.externalUrl)
            showQueries.lastInsertRowId()
        }
    }

    override suspend fun updateShow(update: ShowUpdate): Boolean {
        return runCatching {
            partialUpdateShow(update)
        }
            .isSuccess
    }
    private suspend fun partialUpdateShow(vararg updates: ShowUpdate) {
        return handler.await {
            updates.forEach { update ->
                showQueries.update(
                    title = update.title,
                    posterUrl = update.posterUrl,
                    posterLastUpdated = update.posterLastUpdated,
                    favorite = update.favorite,
                    externalUrl = update.externalUrl,
                    showId = update.showId
                )
            }
        }
    }
}
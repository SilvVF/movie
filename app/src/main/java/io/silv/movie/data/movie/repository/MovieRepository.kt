package io.silv.movie.data.movie.repository

import io.silv.movie.core.Status
import io.silv.movie.data.movie.MovieMapper
import io.silv.movie.data.movie.model.Movie
import io.silv.movie.data.movie.model.MoviePoster
import io.silv.movie.data.movie.model.MovieUpdate
import io.silv.movie.database.DatabaseHandler
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    suspend fun getMovieById(id: Long): Movie?
    fun observeMoviePartialById(id: Long): Flow<MoviePoster>
    fun observeMoviePartialByIdOrNull(id: Long): Flow<MoviePoster?>
    fun observeMovieById(id: Long): Flow<Movie>
    fun observeMovieByIdOrNull(id: Long): Flow<Movie?>
    suspend fun insertMovie(movie: Movie): Long?
    suspend fun updateMovie(update: MovieUpdate): Boolean
    fun observeFavorites(query: String): Flow<List<Movie>>
}

class MovieRepositoryImpl(
    private val handler: DatabaseHandler
): MovieRepository {

    override suspend fun getMovieById(id: Long): Movie? {
        return handler.awaitOneOrNull { movieQueries.selectById(id, MovieMapper.mapMovie) }
    }

    override fun observeMoviePartialById(id: Long): Flow<MoviePoster> {
        return handler.subscribeToOne { movieQueries.selectMoviePartialById(id, MovieMapper.mapMoviePoster) }
    }

    override fun observeMoviePartialByIdOrNull(id: Long): Flow<MoviePoster?> {
        return handler.subscribeToOneOrNull { movieQueries.selectMoviePartialById(id, MovieMapper.mapMoviePoster) }
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
                movie.popularity,
                movie.status?.let { Status.entries.indexOf(it).toLong() },
                movie.productionCompanies
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

    override fun observeFavorites(query: String): Flow<List<Movie>> {
        val q = query.takeIf { it.isNotBlank() }?.let { "%$query%" } ?: ""
        return handler.subscribeToList { movieQueries.selectFavorites(q, MovieMapper.mapMovie) }
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
                    genreIds = update.genreIds?.joinToString(separator = ","),
                    genres = update.genres?.joinToString(separator = ","),
                    originalLanguage = update.originalLanguage,
                    voteCount = update.voteCount?.toLong(),
                    releaseDate = update.releaseDate,
                    popularity = update.popularity,
                    status = update.status?.let { Status.entries.indexOf(it).toLong() },
                    productionCompanies = update.productionCompanies?.joinToString()
                )
            }
        }
    }
}
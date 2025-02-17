package io.silv.movie.data.local

import io.silv.movie.core.SMovie
import io.silv.movie.core.Status
import io.silv.movie.data.model.Movie
import io.silv.movie.data.model.MoviePoster
import io.silv.movie.data.model.MovieUpdate
import io.silv.movie.database.DatabaseHandler
import io.silv.movie.presentation.covers.cache.MovieCoverCache
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock


interface MovieRepository {
    suspend fun getMovieById(id: Long): Movie?
    fun observeMoviePartialById(id: Long): Flow<MoviePoster>
    fun observeMoviePartialByIdOrNull(id: Long): Flow<MoviePoster?>
    fun observeMovieById(id: Long): Flow<Movie>
    fun observeMovieByIdOrNull(id: Long): Flow<Movie?>
    suspend fun insertMovie(movie: Movie): Long?
    suspend fun updateMovie(update: MovieUpdate): Boolean
    fun observeFavoriteMovies(query: String): Flow<List<Movie>>
}


suspend fun MovieRepository.awaitUpdateCoverLastModified(id: Long): Boolean = updateMovie(
    MovieUpdate(
        movieId = id,
        posterLastUpdated = Clock.System.now().epochSeconds
    )
)

suspend fun MovieRepository.awaitUpdateFromSource(
    local: Movie,
    network: SMovie,
    cache: MovieCoverCache,
    manualFetch: Boolean = false
): Boolean {
    val remoteTitle = try {
        network.title
    } catch (_: UninitializedPropertyAccessException) {
        ""
    }

    // if the movie isn't a favorite, set its title from source and update in db
    val title = if (remoteTitle.isEmpty() || local.favorite) null else remoteTitle

    val coverLastModified =
        when {
            // Never refresh covers if the url is empty to avoid "losing" existing covers
            network.posterPath.isNullOrEmpty() -> null
            !manualFetch && local.posterUrl == network.posterPath -> null
            cache.getCustomCoverFile(local.id).exists() -> {
                cache.deleteFromCache(local, false)
                null
            }

            else -> {
                cache.deleteFromCache(local, false)
                Clock.System.now().toEpochMilliseconds()
            }
        }

    val thumbnailUrl = network.posterPath?.takeIf { it.isNotEmpty() }

    return updateMovie(
        MovieUpdate(
            movieId = local.id,
            title = title,
            posterLastUpdated = coverLastModified,
            productionCompanies = network.productionCompanies,
            overview = network.overview,
            genres = network.genres?.map { it.second },
            posterUrl = thumbnailUrl,
            status = network.status,
            externalUrl = network.url,
            genreIds = network.genreIds,
            originalLanguage = network.originalLanguage,
            popularity = network.popularity,
            voteCount = network.voteCount,
            releaseDate = network.releaseDate,
            favorite = null
        ),
    )
}


suspend fun MovieRepository.networkToLocalMovie(movie: Movie): Movie {
    val localMovie = getMovieById(movie.id)
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
                productionCompanies = movie.productionCompanies
                    ?: localMovie.productionCompanies,
            )

        else -> localMovie
    }
}

class MovieRepositoryImpl(
    private val handler: DatabaseHandler
) : MovieRepository {

    override suspend fun getMovieById(id: Long): Movie? {
        return handler.awaitOneOrNull { movieQueries.selectById(id, MovieMapper.mapMovie) }
    }

    override fun observeMoviePartialById(id: Long): Flow<MoviePoster> {
        return handler.subscribeToOne {
            movieQueries.selectMoviePartialById(
                id,
                MovieMapper.mapMoviePoster
            )
        }
    }

    override fun observeMoviePartialByIdOrNull(id: Long): Flow<MoviePoster?> {
        return handler.subscribeToOneOrNull {
            movieQueries.selectMoviePartialById(
                id,
                MovieMapper.mapMoviePoster
            )
        }
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

    override fun observeFavoriteMovies(query: String): Flow<List<Movie>> {
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


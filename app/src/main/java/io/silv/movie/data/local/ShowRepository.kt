package io.silv.movie.data.local


import io.silv.Show
import io.silv.movie.core.SShow
import io.silv.movie.core.Status
import io.silv.movie.core.suspendRunCatching
import io.silv.movie.data.model.TVShow
import io.silv.movie.data.model.TVShowPoster
import io.silv.movie.data.model.TVShowUpdate
import io.silv.movie.database.DatabaseHandler
import io.silv.movie.presentation.covers.cache.TVShowCoverCache
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock


interface ShowRepository {
    suspend fun getShowById(id: Long): TVShow?
    fun observeShowPartialById(id: Long): Flow<TVShowPoster?>
    fun observeShowById(id: Long): Flow<TVShow?>
    suspend fun insertShow(show: TVShow): Long?
    suspend fun updateShow(update: TVShowUpdate): Boolean
    fun observeFavoriteShows(query: String): Flow<List<TVShow>>
}

suspend fun ShowRepository.networkToLocalShow(show: TVShow): TVShow {
    val localShow = getShowById(show.id)
    return when {
        localShow == null -> {
            val id = insertShow(show)
            show.copy(id = id!!)
        }
        !localShow.favorite ->
            localShow.copy(
                title = show.title
            )
        else -> localShow
    }
}

suspend fun ShowRepository.awaitUpdateCoverLastModified(id: Long): Boolean {
    return updateShow(
        TVShowUpdate(
            showId = id,
            posterLastUpdated = Clock.System.now().epochSeconds
        )
    )
}

suspend fun ShowRepository.awaitUpdateFromSource(
    local: TVShow,
    network: SShow,
    cache: TVShowCoverCache,
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

    return updateShow(
        TVShowUpdate(
            showId = local.id,
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

class ShowRepositoryImpl(
    private val handler: DatabaseHandler
): ShowRepository {

    override suspend fun getShowById(id: Long): TVShow? {
        return handler.awaitOneOrNull { showQueries.selectById(id, ShowMapper.mapShow) }
    }

    override fun observeShowPartialById(id: Long): Flow<TVShowPoster?> {
        return handler.subscribeToOneOrNull { showQueries.selectShowPartialById(id,
            ShowMapper.mapShowPoster
        ) }
    }

    override fun observeShowById(id: Long): Flow<TVShow?> {
        return handler.subscribeToOneOrNull { showQueries.selectById(id, ShowMapper.mapShow) }
    }

    override suspend fun insertShow(show: TVShow): Long? {
        return handler.awaitOneOrNullExecutable(inTransaction = true) {
            showQueries.insert(
                show.id,
                show.title,
                show.overview,
                show.genres.ifEmpty { null },
                show.genreIds.ifEmpty { null },
                show.originalLanguage,
                show.voteCount.toLong(),
                show.releaseDate,
                show.posterUrl,
                show.posterLastUpdated,
                show.favorite,
                show.externalUrl,
                show.popularity,
                show.status?.let { Status.entries.indexOf(it).toLong() }
            )
            showQueries.lastInsertRowId()
        }
    }

    override suspend fun updateShow(update: TVShowUpdate): Boolean {
        return suspendRunCatching {
            partialUpdateShow(update)
        }
            .isSuccess
    }

    override fun observeFavoriteShows(query: String): Flow<List<TVShow>> {
        val q = query.takeIf { it.isNotBlank() }?.let { "%$query%" } ?: ""
        return handler.subscribeToList { showQueries.selectFavorites(q, ShowMapper.mapShow) }
    }

    private suspend fun partialUpdateShow(vararg updates: TVShowUpdate) {
        return handler.await {
            updates.forEach { update ->
                showQueries.update(
                    title = update.title,
                    posterUrl = update.posterUrl,
                    posterLastUpdated = update.posterLastUpdated,
                    favorite = update.favorite,
                    externalUrl = update.externalUrl,
                    showId = update.showId,
                    overview = update.overview,
                    genreIds = update.genreIds?.joinToString(separator = ","),
                    genres = update.genres?.joinToString(separator = ","),
                    originalLanguage = update.originalLanguage,
                    voteCount = update.voteCount?.toLong(),
                    releaseDate = update.releaseDate,
                    popularity = update.popularity,
                    status = update.status?.let { Status.entries.indexOf(it).toLong() },
                    productionCompanies = update.productionCompanies?.joinToString(separator = ",")
                )
            }
        }
    }
}
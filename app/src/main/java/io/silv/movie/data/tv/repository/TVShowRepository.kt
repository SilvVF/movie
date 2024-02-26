package io.silv.movie.data.tv.repository

import io.silv.movie.data.tv.ShowMapper
import io.silv.movie.data.tv.TVShow
import io.silv.movie.data.tv.TVShowUpdate
import kotlinx.coroutines.flow.Flow

interface ShowRepository {
    suspend fun getShowById(id: Long): TVShow?
    fun observeShowById(id: Long): Flow<TVShow>
    fun observeShowByIdOrNull(id: Long): Flow<TVShow?>
    suspend fun insertShow(show: TVShow): Long?
    suspend fun updateShow(update: TVShowUpdate): Boolean
}

class ShowRepositoryImpl(
    private val handler: io.silv.movie.database.DatabaseHandler
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

    override suspend fun updateShow(update: TVShowUpdate): Boolean {
        return runCatching {
            partialUpdateShow(update)
        }
            .isSuccess
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
                    showId = update.showId
                )
            }
        }
    }
}
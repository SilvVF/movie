package io.silv.movie.data.lists

import io.silv.movie.database.DatabaseHandler
import kotlinx.coroutines.flow.Flow

interface ContentListRepository {
    fun observeLibraryItems(query: String): Flow<List<ContentListItem>>
    suspend fun deleteList(contentList: ContentList)
    suspend fun createList(name: String)
    suspend fun updateList(update: ContentListUpdate)
    suspend fun addMovieToList(movieId: Long, contentList: ContentList)
    suspend fun removeMovieFromList(movieId: Long, contentList: ContentList)
    suspend fun addShowToList(showId: Long, contentList: ContentList)
    suspend fun removeShowFromList(showId: Long, contentList: ContentList)
}

class ContentListRepositoryImpl(
    private val handler: DatabaseHandler
): ContentListRepository {

    override suspend fun createList(name: String) {
        handler.await { contentListQueries.insert(name) }
    }

    override suspend fun updateList(update: ContentListUpdate) {
        handler.await { contentListQueries.update(update.name, update.id) }
    }

    override suspend fun addMovieToList(movieId: Long, contentList: ContentList) {
        handler.await { contentListJunctionQueries.insert(movieId, null, contentList.id) }
    }

    override suspend fun removeMovieFromList(movieId: Long, contentList: ContentList) {
        handler.await { contentListJunctionQueries.deleteMovieFromList(movieId, contentList.id) }
    }

    override suspend fun addShowToList(showId: Long, contentList: ContentList) {
        handler.await { contentListJunctionQueries.insert(null, showId, contentList.id) }
    }

    override suspend fun removeShowFromList(showId: Long, contentList: ContentList) {
        handler.await { contentListJunctionQueries.deleteShowFromList(showId, contentList.id) }
    }

    override fun observeLibraryItems(query: String): Flow<List<ContentListItem>> {
        val q = query.takeIf { it.isNotBlank() }?.let { "%$query%" } ?: ""
        return handler.subscribeToList { contentListViewQueries.contentlist(q, ContentListMapper.mapListItem) }
    }

    override suspend fun deleteList(contentList: ContentList) {
        return handler.await { contentListQueries.delete(contentList.id) }
    }
}


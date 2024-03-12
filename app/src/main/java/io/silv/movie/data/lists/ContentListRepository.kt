package io.silv.movie.data.lists

import io.silv.movie.database.DatabaseHandler
import kotlinx.coroutines.flow.Flow

interface ContentListRepository {
    fun observeLibraryItems(query: String): Flow<List<ContentListItem>>
    fun observeListCount(): Flow<Long>
    fun observeListById(id: Long): Flow<ContentList?>
    fun observeListItemsByListId(id: Long, query: String, order: String): Flow<List<ContentItem>>
    suspend fun deleteList(contentList: ContentList)
    suspend fun getList(id: Long): ContentList
    suspend fun getListItems(id: Long): List<ContentItem>
    suspend fun createList(name: String): Long
    suspend fun updateList(update: ContentListUpdate)
    suspend fun addMovieToList(movieId: Long, contentList: ContentList)
    suspend fun removeMovieFromList(movieId: Long, contentList: ContentList)
    suspend fun addShowToList(showId: Long, contentList: ContentList)
    suspend fun removeShowFromList(showId: Long, contentList: ContentList)
}

class ContentListRepositoryImpl(
    private val handler: DatabaseHandler
): ContentListRepository {

//    init {
//        GlobalScope.launch {
//            val movies = handler.awaitList { movieQueries.selectAll() }
//            repeat(3) {
//                val list = handler.awaitOneExecutable {
//                    contentListQueries.insert("Items list")
//                    contentListQueries.lastInsertRowId()
//                }
//                handler.await {
//                    movies.take(10 * it).forEach {
//                        contentListJunctionQueries.insert(it.id, null, list)
//                    }
//                }
//            }
//        }
//    }

    override suspend fun createList(name: String): Long {
        return handler.awaitOneExecutable(inTransaction = true) {
            contentListQueries.insert(name)
            contentListQueries.lastInsertRowId()
        }
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

    override fun observeListCount(): Flow<Long> {
        return handler.subscribeToOne { contentListQueries.listCount() }
    }

    override fun observeListById(id: Long): Flow<ContentList?> {
        return handler.subscribeToOneOrNull { contentListQueries.selectById(id, ContentListMapper.mapList) }
    }

    override suspend fun getListItems(id: Long): List<ContentItem> {
        return handler.awaitList { contentListJunctionQueries.selectByListId(id, "", "", ContentListMapper.mapItem) }
    }

    override fun observeListItemsByListId(id: Long, query: String, order: String): Flow<List<ContentItem>> {
        val q = query.takeIf { it.isNotBlank() }?.let { "%$query%" } ?: ""
        return handler.subscribeToList { contentListJunctionQueries.selectByListId(id, q, order, ContentListMapper.mapItem) }
    }

    override suspend fun deleteList(contentList: ContentList) {
        return handler.await { contentListQueries.delete(contentList.id) }
    }

    override suspend fun getList(id: Long): ContentList {
        return handler.awaitOne { contentListQueries.selectById(id, ContentListMapper.mapList) }
    }
}


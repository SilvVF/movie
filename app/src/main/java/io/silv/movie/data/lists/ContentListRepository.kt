package io.silv.movie.data.lists

import io.silv.movie.data.cache.ListCoverCache
import io.silv.movie.database.DatabaseHandler
import io.silv.movie.presentation.library.view.favorite.FavoritesSortMode
import io.silv.movie.presentation.library.view.list.ListSortMode
import kotlinx.coroutines.flow.Flow

interface ContentListRepository {
    fun observeLibraryItems(query: String): Flow<List<ContentListItem>>
    fun observeListCount(): Flow<Long>
    fun observeListById(id: Long): Flow<ContentList?>
    fun observeListItemsByListId(id: Long, query: String, sortMode: ListSortMode): Flow<List<ContentItem>>
    fun observeFavorites(query: String, sortMode: FavoritesSortMode): Flow<List<ContentItem>>
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
    private val handler: DatabaseHandler,
    private val listCoverCache: ListCoverCache
): ContentListRepository {

    override suspend fun createList(name: String): Long {
        return handler.awaitOneExecutable(inTransaction = true) {
            contentListQueries.insert(name)
            contentListQueries.lastInsertRowId()
        }
    }

    override suspend fun updateList(update: ContentListUpdate) {
        handler.await { contentListQueries.update(update.name, update.posterLastUpdated,  update.id) }
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

    override fun observeListItemsByListId(
        id: Long,
        query: String,
        sortMode: ListSortMode,
    ): Flow<List<ContentItem>> {
        val q = query.takeIf { it.isNotBlank() }?.let { "%$query%" } ?: ""
        return handler.subscribeToList {
            when(sortMode) {
                ListSortMode.Movie -> contentListJunctionQueries.selectMoviesByListId(id, q, ContentListMapper.mapItem)
                ListSortMode.Show -> contentListJunctionQueries.selectShowsByListId(id, q, ContentListMapper.mapItem)
                ListSortMode.RecentlyAdded -> contentListJunctionQueries.selectByListId(id, q, "recent", ContentListMapper.mapItem)
                ListSortMode.Title -> contentListJunctionQueries.selectByListId(id, q, "title", ContentListMapper.mapItem)
            }
        }
    }

    override fun observeFavorites(query: String, sortMode: FavoritesSortMode): Flow<List<ContentItem>> {
        val q = query.takeIf { it.isNotBlank() }?.let { "%$query%" } ?: ""
        return handler.subscribeToList {
            when (sortMode) {
                FavoritesSortMode.Movie -> favoritesViewQueries.favoritesOrderByMovieOrShow(0L, q, 1L,  ContentListMapper.mapFavoriteItem)
                FavoritesSortMode.RecentlyAdded -> favoritesViewQueries.favoritesOrderByRecent(q, ContentListMapper.mapFavoriteItem)
                FavoritesSortMode.Show -> favoritesViewQueries.favoritesOrderByMovieOrShow(1L, q, 0L,  ContentListMapper.mapFavoriteItem)
                FavoritesSortMode.Title -> favoritesViewQueries.favoritesOrderByTitle(q,  ContentListMapper.mapFavoriteItem)
            }
        }
    }


    override suspend fun deleteList(contentList: ContentList) {
        listCoverCache.deleteCustomCover(contentList.id)
        return handler.await { contentListQueries.delete(contentList.id) }
    }

    override suspend fun getList(id: Long): ContentList {
        return handler.awaitOne { contentListQueries.selectById(id, ContentListMapper.mapList) }
    }
}


package io.silv.movie.data.lists

import io.silv.movie.data.cache.ListCoverCache
import io.silv.movie.database.DatabaseHandler
import io.silv.movie.presentation.library.screenmodels.FavoritesSortMode
import io.silv.movie.presentation.library.screenmodels.ListSortMode
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

interface ContentListRepository {
    fun observeLibraryItems(query: String): Flow<List<ContentListItem>>
    fun observeListCount(): Flow<Long>
    fun observeListById(id: Long): Flow<ContentList?>
    fun observeListItemsByListId(id: Long, query: String, sortMode: ListSortMode): Flow<List<ContentItem>>
    fun observeFavorites(query: String, sortMode: FavoritesSortMode): Flow<List<ContentItem>>
    suspend fun deleteList(contentList: ContentList)
    suspend fun getList(id: Long): ContentList?
    suspend fun getListForSupabaseId(supabaseId: String): ContentList?
    suspend fun getListItems(id: Long): List<ContentItem>
    suspend fun createList(
        name: String,
        supabaseId: String? = null,
        userId: String? = null,
        createdAt: Long? = null,
        inLibrary: Boolean = false
    ): Long
    suspend fun updateList(update: ContentListUpdate)
    suspend fun addItemsToList(items: List<Pair<Long, Boolean>>, contentList: ContentList)
    suspend fun addMovieToList(movieId: Long, contentList: ContentList)
    suspend fun removeMovieFromList(movieId: Long, contentList: ContentList)
    suspend fun addShowToList(showId: Long, contentList: ContentList)
    suspend fun removeShowFromList(showId: Long, contentList: ContentList)
}

class ContentListRepositoryImpl(
    private val handler: DatabaseHandler,
    private val listCoverCache: ListCoverCache,
): ContentListRepository {

    override suspend fun createList(
        name: String,
        supabaseId: String?,
        userId: String?,
        createdAt: Long?,
        inLibrary: Boolean
    ): Long {
        return handler.awaitOneExecutable(inTransaction = true) {
            contentListQueries.insert(name, createdAt ?: Clock.System.now().toEpochMilliseconds(), supabaseId, userId, inLibrary)
            contentListQueries.lastInsertRowId()
        }
    }

    override suspend fun updateList(update: ContentListUpdate) {
        handler.await {
            contentListQueries.update(
                update.name,
                update.posterLastUpdated,
                update.description,
                update.username,
                update.inLibrary,
                update.public,
                update.id
            )
        }
    }

    override suspend fun addItemsToList(
        items: List<Pair<Long, Boolean>>,
        contentList: ContentList
    ) {
        handler.await(true) {
            items.forEach { (id, isMovie) ->
                if (isMovie) {
                    contentItemQueries.insert(id, -1, contentList.id)
                } else {
                    contentItemQueries.insert(-1, id, contentList.id)
                }
            }
        }
    }

    override suspend fun addMovieToList(movieId: Long, contentList: ContentList) {
        handler.await { contentItemQueries.insert(movieId, -1, contentList.id) }
    }

    override suspend fun removeMovieFromList(movieId: Long, contentList: ContentList) {
        handler.await { contentItemQueries.deleteMovieFromList(movieId, contentList.id) }
    }

    override suspend fun addShowToList(showId: Long, contentList: ContentList) {
        handler.await { contentItemQueries.insert(-1, showId, contentList.id) }
    }

    override suspend fun removeShowFromList(showId: Long, contentList: ContentList) {
        handler.await { contentItemQueries.deleteShowFromList(showId, contentList.id) }
    }

    override fun observeLibraryItems(query: String): Flow<List<ContentListItem>> {
        val q = query.takeIf { it.isNotBlank() }?.let { "%$query%" } ?: ""
        return handler.subscribeToList { contentListViewQueries.libraryContentList(q, ContentListMapper.mapListItem) }
    }

    override fun observeListCount(): Flow<Long> {
        return handler.subscribeToOne { contentListQueries.listCount() }
    }

    override fun observeListById(id: Long): Flow<ContentList?> {
        return handler.subscribeToOneOrNull { contentListQueries.selectById(id, ContentListMapper.mapList) }
    }

    override suspend fun getListItems(id: Long): List<ContentItem> {
        return handler.awaitList { contentItemQueries.selectByListId(id, "", "", ContentListMapper.mapItem) }
    }

    override fun observeListItemsByListId(
        id: Long,
        query: String,
        sortMode: ListSortMode,
    ): Flow<List<ContentItem>> {
        val q = query.takeIf { it.isNotBlank() }?.let { "%$query%" } ?: ""
        return handler.subscribeToList {
            when(sortMode) {
                ListSortMode.Movie -> contentItemQueries.selectMoviesByListId(id, q, ContentListMapper.mapItem)
                ListSortMode.Show -> contentItemQueries.selectShowsByListId(id, q, ContentListMapper.mapItem)
                ListSortMode.RecentlyAdded -> contentItemQueries.selectByListId(id, q, "recent", ContentListMapper.mapItem)
                ListSortMode.Title -> contentItemQueries.selectByListId(id, q, "title", ContentListMapper.mapItem)
            }
        }
    }

    override fun observeFavorites(query: String, sortMode: FavoritesSortMode): Flow<List<ContentItem>> {
        val q = query.takeIf { it.isNotBlank() }?.let { "%$query%" } ?: ""
        return handler.subscribeToList {
            when (sortMode) {
                FavoritesSortMode.Movie -> favoritesViewQueries.favoritesOrderByMovieOrShow(0L, q, ContentListMapper.mapFavoriteItem)
                FavoritesSortMode.RecentlyAdded -> favoritesViewQueries.favoritesOrderByRecent(q, ContentListMapper.mapFavoriteItem)
                FavoritesSortMode.Show -> favoritesViewQueries.favoritesOrderByMovieOrShow(1L, q, ContentListMapper.mapFavoriteItem)
                FavoritesSortMode.Title -> favoritesViewQueries.favoritesOrderByTitle(q,  ContentListMapper.mapFavoriteItem)
            }
        }
    }


    override suspend fun deleteList(contentList: ContentList) {
        listCoverCache.deleteCustomCover(contentList.id)
        return handler.await { contentListQueries.deleteById(contentList.id) }
    }

    override suspend fun getList(id: Long): ContentList? {
        return handler.awaitOneOrNull { contentListQueries.selectById(id, ContentListMapper.mapList) }
    }

    override suspend fun getListForSupabaseId(supabaseId: String): ContentList? {
        return handler.awaitOneOrNull { contentListQueries.selectBySupabaseId(supabaseId, ContentListMapper.mapList) }
    }
}


package io.silv.movie.data.content.lists.repository

import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.data.content.lists.ContentListItem
import io.silv.movie.data.content.lists.ContentListMapper
import io.silv.movie.data.content.lists.ContentListUpdate
import io.silv.movie.database.DatabaseHandler
import io.silv.movie.presentation.covers.cache.ListCoverCache
import io.silv.movie.presentation.screenmodel.FavoritesSortMode
import io.silv.movie.presentation.screenmodel.ListSortMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

interface ContentListRepository {
    fun observeLibraryItems(query: String): Flow<List<Pair<ContentList, List<ContentItem>>>>
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
        subscribers: Long? = null,
        inLibrary: Boolean = false
    ): Long
    suspend fun updateList(update: ContentListUpdate)
    suspend fun addItemsToList(items: List<Pair<Long, Boolean>>, contentList: ContentList)
    suspend fun addMovieToList(movieId: Long, contentList: ContentList, createdAt: Long?)
    suspend fun removeMovieFromList(movieId: Long, contentList: ContentList)
    suspend fun addShowToList(showId: Long, contentList: ContentList, createdAt: Long?)
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
        subscribers: Long?,
        inLibrary: Boolean
    ): Long {
        return handler.awaitOneExecutable(inTransaction = true) {
            contentListQueries.insert(
                name = name,
                createdAt = createdAt ?: Clock.System.now().epochSeconds,
                supabaseId = supabaseId,
                createdBy  = userId,
                inLibrary = inLibrary,
                subscribers = subscribers ?: 0
            )
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
                update.subscribers,
                update.pinned,
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
                    contentItemQueries.insert(id, -1, contentList.id, null)
                } else {
                    contentItemQueries.insert(-1, id, contentList.id, null)
                }
            }
        }
    }

    override suspend fun addMovieToList(movieId: Long, contentList: ContentList, createdAt: Long?) {
        handler.await { contentItemQueries.insert(movieId, -1, contentList.id, createdAt.toString()) }
    }

    override suspend fun removeMovieFromList(movieId: Long, contentList: ContentList) {
        handler.await { contentItemQueries.deleteMovieFromList(movieId, contentList.id) }
    }

    override suspend fun addShowToList(showId: Long, contentList: ContentList, createdAt: Long?) {
        handler.await { contentItemQueries.insert(-1, showId, contentList.id, createdAt.toString()) }
    }

    override suspend fun removeShowFromList(showId: Long, contentList: ContentList) {
        handler.await { contentItemQueries.deleteShowFromList(showId, contentList.id) }
    }

    override fun observeLibraryItems(query: String): Flow<List<Pair<ContentList, List<ContentItem>>>> {
        val q = query.takeIf { it.isNotBlank() }?.let { "%$query%" } ?: ""
        return handler.subscribeToList { contentListViewQueries.libraryContentList(q,ContentListMapper.mapListItem) }
            .map { contentListItems ->
                contentListItems.groupBy { it.list.id }
                    .map { (_, v) ->
                        Pair(
                            v.first().list,
                            v.filterIsInstance<ContentListItem.Item>()
                                .sortedBy { it.createdAt }
                                .map { it.contentItem }
                        )
                    }

            }
    }

    override fun observeListCount(): Flow<Long> {
        return handler.subscribeToOne { contentListQueries.listCount() }
    }

    override fun observeListById(id: Long): Flow<ContentList?> {
        return handler.subscribeToOneOrNull { contentListQueries.selectById(id,
            ContentListMapper.mapList
        ) }
    }

    override suspend fun getListItems(id: Long): List<ContentItem> {
        return handler.awaitList { contentItemQueries.selectByListId(id, "", "",
            ContentListMapper.mapItem
        ) }
    }

    override fun observeListItemsByListId(
        id: Long,
        query: String,
        sortMode: ListSortMode,
    ): Flow<List<ContentItem>> {
        val q = query.takeIf { it.isNotBlank() }?.let { "%$query%" } ?: ""
        return handler.subscribeToList {
            if (sortMode.ascending) {
                when(sortMode) {
                    is ListSortMode.Movie -> contentItemQueries.selectMoviesByListId(id, q,
                        ContentListMapper.mapItem
                    )
                    is ListSortMode.Show -> contentItemQueries.selectShowsByListId(id, q,
                        ContentListMapper.mapItem
                    )
                    is ListSortMode.RecentlyAdded -> contentItemQueries.selectByListIdAsc(id, q, "recent",
                        ContentListMapper.mapItem
                    )
                    is ListSortMode.Title -> contentItemQueries.selectByListIdAsc(id, q, "title",
                        ContentListMapper.mapItem
                    )
                }
            } else {
                when(sortMode) {
                    is ListSortMode.Movie -> contentItemQueries.selectMoviesByListId(id, q,
                        ContentListMapper.mapItem
                    )
                    is ListSortMode.Show -> contentItemQueries.selectShowsByListId(id, q,
                        ContentListMapper.mapItem
                    )
                    is ListSortMode.RecentlyAdded -> contentItemQueries.selectByListId(id, q, "recent",
                        ContentListMapper.mapItem
                    )
                    is ListSortMode.Title -> contentItemQueries.selectByListId(id, q, "title",
                        ContentListMapper.mapItem
                    )
                }
            }
        }
    }

    override fun observeFavorites(query: String, sortMode: FavoritesSortMode): Flow<List<ContentItem>> {
        val q = query.takeIf { it.isNotBlank() }?.let { "%$query%" } ?: ""
        return handler.subscribeToList {
            when (sortMode) {
                FavoritesSortMode.Movie -> favoritesViewQueries.favoritesOrderByMovieOrShow(0L, q,
                    ContentListMapper.mapFavoriteItem
                )
                FavoritesSortMode.RecentlyAdded -> favoritesViewQueries.favoritesOrderByRecent(q,
                    ContentListMapper.mapFavoriteItem
                )
                FavoritesSortMode.Show -> favoritesViewQueries.favoritesOrderByMovieOrShow(1L, q,
                    ContentListMapper.mapFavoriteItem
                )
                FavoritesSortMode.Title -> favoritesViewQueries.favoritesOrderByTitle(q,
                    ContentListMapper.mapFavoriteItem
                )
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
        return handler.awaitOneOrNull { contentListQueries.selectBySupabaseId(supabaseId,
            ContentListMapper.mapList
        ) }
    }
}


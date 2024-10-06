package io.silv.movie.data.user.repository

import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.data.content.lists.ListWithPostersRpcResponse
import io.silv.movie.data.content.movie.model.Movie
import io.silv.movie.data.content.tv.model.TVShow
import io.silv.movie.data.user.SupabaseConstants.FAVORITE_MOVIES
import io.silv.movie.data.user.SupabaseConstants.LIST_ITEM
import io.silv.movie.data.user.SupabaseConstants.RPC.moreFromSubscribed
import io.silv.movie.data.user.SupabaseConstants.RPC.subscribedListWithItems
import io.silv.movie.data.user.SupabaseConstants.SUBSCRIPTION
import io.silv.movie.data.user.SupabaseConstants.USER_LIST
import io.silv.movie.data.user.model.FavoriteMovie
import io.silv.movie.data.user.model.FavoriteMovieInsert
import io.silv.movie.data.user.model.list.ListItem
import io.silv.movie.data.user.model.list.ListWithItems
import io.silv.movie.data.user.model.list.UserList
import io.silv.movie.data.user.model.list.UserListUpdate
import io.silv.movie.data.user.model.subscription.Subscription
import io.silv.movie.data.user.model.subscription.SubscriptionWithItem
import io.silv.movie.data.user.model.toListWithItems
import kotlinx.datetime.Clock
import timber.log.Timber
import java.util.UUID


class ListRepository(
    private val postgrest: Postgrest,
    private val auth: Auth,
) {
    suspend fun selectFavoritesList(userId: String): List<FavoriteMovie>? {
        return runCatching {
            postgrest[FAVORITE_MOVIES]
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<FavoriteMovie>()
        }
            .getOrNull()
    }

    suspend fun unsubscribeFromList(id: String): Result<Unit> {
        return runCatching {
            postgrest[SUBSCRIPTION]
                .delete {
                   filter {
                       eq("list_id", id)
                       eq("user_id", auth.currentUserOrNull()?.id!!)
                   }
                }
        }
    }

    suspend fun subscribeToList(id: String): Result<Unit> {
        return runCatching {
            postgrest["subscription"]
                .insert(
                    Subscription(auth.currentUserOrNull()!!.id, id)
                )
        }
    }

    suspend fun addShowToFavorites(show: TVShow): Boolean {
        return runCatching {
            postgrest[FAVORITE_MOVIES]
                .insert(
                    FavoriteMovieInsert(
                        auth.currentUserOrNull()!!.id,
                        show.posterUrl?.substringAfterLast('/'),
                        show.title,
                        show.overview,
                        show.popularity,
                        -1,
                        show.id,
                    )
                )
        }
            .isSuccess
    }

    suspend fun addMovieToFavoritesList(movie: Movie): Boolean {
        return runCatching {
            postgrest[FAVORITE_MOVIES]
                .insert(
                    FavoriteMovieInsert(
                        auth.currentUserOrNull()!!.id,
                        movie.posterUrl?.substringAfterLast('/'),
                        movie.title,
                        movie.overview,
                        movie.popularity,
                         movie.id,
                        -1
                    )
                )
        }
            .isSuccess
    }

    suspend fun deleteShowFromFavorites(showId: Long): Boolean {
        return runCatching {
            postgrest[FAVORITE_MOVIES]
                .delete {
                    filter {
                        eq("user_id", auth.currentUserOrNull()!!.id)
                        eq("show_id", showId)
                    }
                }
        }
            .isSuccess
    }

    suspend fun selectAllItemsForList(listId: String): List<ListItem>? {
        return runCatching {
            postgrest[LIST_ITEM]
                .select {
                    filter { eq("list_id", listId) }
                }
                .decodeList<ListItem>()
        }
            .getOrNull()
    }

    suspend fun selectListWithItemsById(listId: String): ListWithItems? {
        return runCatching {
            postgrest[USER_LIST]
                .select(
                    columns = Columns.raw("*, listitem(*)")
                ) {
                    limit(1)
                    filter { eq("list_id", listId) }
                }
                .decodeSingle<ListWithItems>()
        }
            .onFailure { Timber.e(it) }
            .getOrNull()
    }

    suspend fun selectListById(listId: String): UserList? {
        return runCatching {
            postgrest[USER_LIST]
                .select {
                    filter { eq("list_id", listId) }
                }
                .decodeSingle<UserList>()
        }
            .getOrNull()
    }

    suspend fun selectRecommendedFromSubscriptions(): List<ListWithPostersRpcResponse>? {
        return runCatching {
            val set = mutableSetOf<String>()
            postgrest.moreFromSubscribed(auth.currentUserOrNull()!!.id, limit = 10, offset = 0)
                .decodeList<ListWithPostersRpcResponse>()
                .filter { set.add(it.listId) }
        }
            .onFailure { Timber.e(it) }
            .getOrNull()
    }

    suspend fun selectSubscriptions(userId: String): List<ListWithItems>? {
        return runCatching {
            postgrest.subscribedListWithItems(userId)
                .decodeList<SubscriptionWithItem>()
                .toListWithItems()
        }
            .onFailure { Timber.e(it) }
            .getOrNull()
    }

    suspend fun selectListsByUserId(userId: String): List<ListWithItems>? {
        return runCatching {
            postgrest[USER_LIST]
                .select(
                    Columns.raw("*, listitem(*)")
                ) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<ListWithItems>()
        }
            .getOrNull()
    }

    suspend fun deleteMovieFromFavorites(movieId: Long): Boolean {
        return runCatching {
            postgrest[FAVORITE_MOVIES]
                .delete {
                    filter {
                        eq("user_id", auth.currentUserOrNull()!!.id)
                        eq("movie_id", movieId)
                    }
                }
        }
            .isSuccess
    }

    suspend fun updateList(update: UserListUpdate): Boolean {
        return runCatching {
            Timber.d(update.toString())
            val list = postgrest[USER_LIST]
                .update(
                    {
                        if (update.name != null) { set("name", update.name) }
                        if (update.public != null) { set("public", update.public) }
                        if (update.description != null) { set("description", update.description) }
                    }
                ) {
                    filter {
                        eq("list_id", update.listId)
                    }
                }
            Timber.d(list.toString())
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    suspend fun deleteList(listId: String): Boolean {
        return runCatching {
            postgrest[USER_LIST].delete {
                filter { eq("list_id", listId) }
            }
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    suspend fun insertList(name: String): UserList? {
        val now = Clock.System.now()
        return runCatching {
            postgrest[USER_LIST].insert(
                UserList(
                    listId = UUID.randomUUID().toString(),
                    userId = auth.currentUserOrNull()!!.id,
                    name = name,
                    createdAt = now,
                    updatedAt = now,
                    public = false,
                    description = "",
                    subscribers = 0
                )
            ) {
                select()
            }
                .decodeSingle<UserList>()
        }
            .onFailure { Timber.e(it) }
            .getOrNull()
    }

    suspend fun deleteMovieFromList(movieId: Long , contentList: ContentList): Boolean {
        return runCatching {
            postgrest[LIST_ITEM]
                .delete {
                    filter {
                        eq("movie_id", movieId)
                        eq("list_id", contentList.supabaseId!!)
                        eq("user_id", auth.currentUserOrNull()?.id!!)
                    }
                }
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    suspend fun deleteShowFromList(showId: Long, contentList: ContentList): Boolean {
        return runCatching {
            postgrest[LIST_ITEM]
                .delete {
                    filter {
                        eq("show_id", showId)
                        eq("list_id", contentList.supabaseId!!)
                        eq("user_id", auth.currentUserOrNull()?.id!!)
                    }
                }
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    suspend fun addMovieToList(movieId: Long, posterPath: String?, title: String, contentList: ContentList): Boolean {
        return runCatching {
            postgrest[LIST_ITEM]
                .insert(
                    ListItem(
                        listId = contentList.supabaseId!!,
                        userId = auth.currentUserOrNull()!!.id,
                        movieId = movieId,
                        showId = -1,
                        posterPath = posterPath,
                        title = title,
                        createdAt = Clock.System.now()
                    )
                )
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    suspend fun addShowToList(showId: Long, posterPath: String?, title: String, contentList: ContentList): Boolean {
        return runCatching {
            postgrest[LIST_ITEM]
                .insert(
                    ListItem(
                        listId = contentList.supabaseId!!,
                        userId = auth.currentUserOrNull()!!.id,
                        movieId = -1,
                        showId = showId,
                        posterPath = posterPath,
                        title = title,
                        createdAt = Clock.System.now()
                    )
                )
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

}
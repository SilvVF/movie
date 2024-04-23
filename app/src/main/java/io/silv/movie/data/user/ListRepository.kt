package io.silv.movie.data.user

import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.movie.model.Movie
import io.silv.movie.data.tv.model.TVShow
import io.silv.movie.presentation.browse.lists.ListWithPostersRpcResponse
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.util.UUID

@Serializable
data class UserList(
    @SerialName("list_id")
    val listId: String,
    @SerialName("user_id")
    val userId: String,
    val name: String,
    val description: String,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant?,
    val public: Boolean,
    val subscribers: Long,
)

@Serializable
data class FavoriteMovie(
    @SerialName("user_id")
    val userId: String,
    @SerialName("poster_path")
    val posterPath: String,
    val title: String,
    val overview: String,
    @SerialName("vote_average")
    val voteAverage: Double,
    @SerialName("movie_id")
    val movieId: Long,
    @SerialName("show_id")
    val showId: Long,
    @SerialName("_id")
    val id: Long,
)


@Serializable
data class ListWithItems(
    @SerialName("list_id")
    val listId: String,
    @SerialName("user_id")
    val userId: String,
    val name: String,
    val description: String,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant?,
    val public: Boolean,
    val subscribers: Long,
    @SerialName("listitem")
    val items: List<ListItem>?,
)

@Serializable
data class SubscriptionWithItem(
    @SerialName("list_id")
    val listId: String,
    @SerialName("user_id")
    val userId: String,
    val name: String,
    val description: String,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant?,
    val public: Boolean,
    val subscribers: Long,
    @SerialName("movie_id")
    val movieId: Long? = null,
    @SerialName("show_id")
    val showId: Long? = null,
    @SerialName("poster_path")
    val posterPath: String? = null
)

@Serializable
private data class FavoriteMovieInsert(
    @SerialName("user_id")
    val userId: String,
    @SerialName("poster_path")
    val posterPath: String?,
    val title: String,
    val overview: String,
    @SerialName("vote_average")
    val voteAverage: Double,
    @SerialName("movie_id")
    val movieId: Long,
    @SerialName("show_id")
    val showId: Long,
)

@Serializable
data class ListItem(
    @SerialName("list_id")
    val listId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("movie_id")
    val movieId: Long,
    @SerialName("show_id")
    val showId: Long,
    @SerialName("poster_path")
    val posterPath: String?
)

fun ContentList.toUserListUpdate(): UserListUpdate {
    return UserListUpdate(
        listId = supabaseId!!,
        createdAt = null,
        updatedAt = null,
        description = description,
        name = name,
        public = public
    )
}

data class UserListUpdate(
    val listId: String,
    val name: String? = null,
    val description: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val public: Boolean? = null,
)

@Serializable
private data class SubscriptionRpcParams(
    val uid: String
)


@Serializable
private data class FromSubscribedRpcParams(
    val uid: String,
    val lim: Int
)

@Serializable
private data class Subscription(
    @SerialName("user_id")
    val userId: String,
    @SerialName("list_id")
    val listId: String
)

class ListRepository(
    private val postgrest: Postgrest,
    private val auth: Auth,
) {
    suspend fun selectFavoritesList(userId: String): List<FavoriteMovie>? {
        return runCatching {
            postgrest["favoritemovies"]
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<FavoriteMovie>()
        }
            .getOrNull()
    }

    suspend fun unsubscribeFromList(id: String): Result<Unit> {
        return runCatching {
            postgrest["subscription"]
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
            postgrest["favoritemovies"]
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
            postgrest["favoritemovies"]
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
            postgrest["favoritemovies"]
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
            postgrest.rpc(
                "select_recommended_by_subscriptions",
                FromSubscribedRpcParams(auth.currentUserOrNull()!!.id, lim = 20)
            )
                .decodeList<ListWithPostersRpcResponse>()
        }
            .getOrNull()
    }

    suspend fun selectSubscriptions(userId: String): List<ListWithItems>? {
        return runCatching {
            postgrest.rpc(
                "select_subscribed_lists_with_items",
                parameters = SubscriptionRpcParams(userId)
            )
                .decodeList<SubscriptionWithItem>()
                .groupBy { it.listId }
                .map { (listId, items) ->
                    val first = items.first()
                    ListWithItems(
                        listId = listId,
                        userId = first.userId,
                        description = first.description,
                        public = first.public,
                        name = first.name,
                        createdAt = first.createdAt,
                        updatedAt = first.updatedAt,
                        subscribers = first.subscribers,
                        items = items.mapNotNull {
                            if (it.movieId == null || it.showId == null) null
                            else ListItem(
                                listId,
                                it.userId,
                                it.movieId,
                                it.showId,
                                it.posterPath
                            )
                        }
                    )
                }
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
            postgrest["favoritemovies"]
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

    suspend fun addMovieToList(movieId: Long, posterPath: String?, contentList: ContentList): Boolean {
        return runCatching {
            postgrest[LIST_ITEM]
                .insert(
                    ListItem(
                        listId = contentList.supabaseId!!,
                        userId = auth.currentUserOrNull()!!.id,
                        movieId = movieId,
                        showId = -1,
                        posterPath = posterPath?.substringAfterLast('/')
                    )
                )
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    suspend fun addShowToList(showId: Long, posterPath: String?, contentList: ContentList): Boolean {
        return runCatching {
            postgrest[LIST_ITEM]
                .insert(
                    ListItem(
                        listId = contentList.supabaseId!!,
                        userId = auth.currentUserOrNull()!!.id,
                        movieId = -1,
                        showId = showId,
                        posterPath = posterPath?.substringAfterLast('/')
                    )
                )
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    companion object {
        const val USER_LIST = "userlist"
        const val LIST_ITEM = "listitem"
    }
}
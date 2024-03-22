package io.silv.movie.data.user

import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.silv.movie.data.lists.ContentList
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
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant,
    val public: Boolean,
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
)

fun ContentList.toUserListUpdate(): UserListUpdate {
    return UserListUpdate(
        listId = supabaseId!!,
        createdAt = null,
        updatedAt = null,
        name = name,
        public = public
    )
}

data class UserListUpdate(
    val listId: String,
    val name: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val public: Boolean? = null,
)

class ListRepository(
    private val postgrest: Postgrest,
    private val auth: Auth,
) {

    suspend fun updateList(update: UserListUpdate): Boolean {
        return runCatching {
            Timber.d(update.toString())
            val list = postgrest[USER_LIST]
                .update(
                    {
                        if (update.name != null) { set("name", update.name) }
                        if (update.public != null) { set("public", update.public) }
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
                    public = false
                )
            ) {
                select()
            }
                .decodeSingle<UserList>()
        }
            .onFailure { Timber.e(it) }
            .getOrNull()
    }

    suspend fun addMovieToList(movieId: Long , contentList: ContentList): Boolean {
        return runCatching {
            postgrest[LIST_ITEM]
                .insert(
                    ListItem(
                        listId = contentList.supabaseId!!,
                        userId = auth.currentUserOrNull()!!.id,
                        movieId = movieId,
                        showId = -1
                    )
                )
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    suspend fun addShowToList(showId: Long, contentList: ContentList): Boolean {
        return runCatching {
            postgrest[LIST_ITEM]
                .insert(
                    ListItem(
                        listId = contentList.supabaseId!!,
                        userId = auth.currentUserOrNull()!!.id,
                        movieId = -1,
                        showId = showId
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
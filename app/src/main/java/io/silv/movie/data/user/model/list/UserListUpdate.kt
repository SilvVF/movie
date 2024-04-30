package io.silv.movie.data.user.model.list

import io.silv.movie.data.content.lists.ContentList
import kotlinx.datetime.Instant


data class UserListUpdate(
    val listId: String,
    val name: String? = null,
    val description: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val public: Boolean? = null,
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
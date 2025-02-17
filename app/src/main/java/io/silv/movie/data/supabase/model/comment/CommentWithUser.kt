package io.silv.movie.data.supabase.model.comment

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class CommentWithUser(
    val id: Long,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("user_id")
    val userId: String? = null,
    val message: String,
    val users: Users? = null,
) {

    @Serializable
    data class Users(
        val username: String? = null,
        @SerialName("profile_image")
        val profileImage: String? = null,
    )
}
package io.silv.movie.data.user.model.comment

import androidx.compose.runtime.Stable
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName

@kotlinx.serialization.Serializable
@Stable
data class PagedComment(
    val id: Long,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("user_id")
    val userId: String? = null,
    val message: String,
    @SerialName("movie_id")
    val movieId: Long,
    @SerialName("show_id")
    val showId: Long,
    @SerialName("profile_image")
    val profileImage: String? = null,
    val username: String? = null,
    val likes: Long,
    @SerialName("user_liked")
    val userLiked: Boolean,
    val replies: Long,
    val total: Long,
)

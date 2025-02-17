package io.silv.movie.data.supabase.model.comment

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: Long,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("user_id")
    val userId: String,
    val message: String,
    @SerialName("movie_id")
    val movieId: Long,
    @SerialName("show_id")
    val showId: Long,
    val likes: Long
)




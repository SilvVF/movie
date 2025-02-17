package io.silv.movie.data.supabase.model.list

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val posterPath: String?,
    val title: String,
    val description: String? = null,
    @SerialName("created_at")
    val createdAt: Instant
)

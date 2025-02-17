package io.silv.movie.data.supabase.model.subscription

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


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
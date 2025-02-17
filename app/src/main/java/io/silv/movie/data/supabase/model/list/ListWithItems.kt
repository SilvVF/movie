package io.silv.movie.data.supabase.model.list

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
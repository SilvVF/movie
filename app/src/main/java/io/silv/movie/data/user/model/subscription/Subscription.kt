package io.silv.movie.data.user.model.subscription

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Subscription(
    @SerialName("user_id")
    val userId: String,
    @SerialName("list_id")
    val listId: String
)
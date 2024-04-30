package io.silv.movie.data.user.model.comment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CLike(
    @SerialName("user_id")
    val userId: String,
    val cid: Long
)

package io.silv.movie.data.user.model.comment

import kotlinx.serialization.Serializable


@Serializable
data class SendReply(
    val message: String,
    val cid: Long
)
package io.silv.movie.data.supabase.model.comment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendComment(
    val message: String,
    @SerialName("movie_id")
    val movieId: Long,
    @SerialName("show_id")
    val showId: Long,
)

package io.silv.data.movie.model

import androidx.compose.runtime.Stable

@Stable
data class Movie(
    val id: Long,
    val title: String,
    val externalUrl: String,
    val posterUrl: String?,
    val posterLastUpdated: Long,
    val favorite: Boolean
) {

    companion object {
        fun create() = Movie(
            id = -1L,
            posterUrl = "",
            title = "",
            favorite = false,
            posterLastUpdated = -1L,
            externalUrl = ""
       )
    }
}



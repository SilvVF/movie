package io.silv.data.tv

import androidx.compose.runtime.Stable

@Stable
data class TVShow(
    val id: Long,
    val title: String,
    val externalUrl: String,
    val posterUrl: String?,
    val posterLastUpdated: Long,
    val favorite: Boolean
) {

    companion object {
        fun create() = TVShow(
            id = -1L,
            posterUrl = "",
            title = "",
            favorite = false,
            posterLastUpdated = -1L,
            externalUrl = ""
        )
    }
}
package io.silv.movie.data.tv

import androidx.compose.runtime.Stable
import io.silv.movie.core.STVShow

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

fun STVShow.toDomain(): TVShow {
    return TVShow.create().copy(
        id = id,
        title = title,
        posterUrl = posterPath,
    )
}
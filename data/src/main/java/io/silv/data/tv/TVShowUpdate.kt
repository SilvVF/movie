package io.silv.data.tv

data class TVShowUpdate(
    val showId: Long,
    val title: String? = null,
    val externalUrl: String? = null,
    val posterUrl: String? = null,
    val posterLastUpdated: Long? = null,
    val favorite: Boolean? = null
)

fun TVShow.toShowUpdate(): TVShowUpdate {
    return TVShowUpdate(
        showId = id,
        favorite = favorite,
        title = title,
        externalUrl = externalUrl,
        posterUrl = posterUrl,
        posterLastUpdated = posterLastUpdated
    )
}
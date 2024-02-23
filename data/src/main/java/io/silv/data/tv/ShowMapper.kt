package io.silv.data.tv

import io.silv.core.STVShow


object ShowMapper {

    val mapShow =
        { id: Long, title: String, posterUrl: String?, posterLastUpdated: Long, favorite: Boolean, externalUrl: String ->
            TVShow(
                id = id,
                title = title,
                posterUrl  = posterUrl,
                favorite = favorite,
                posterLastUpdated = posterLastUpdated,
                externalUrl = externalUrl
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
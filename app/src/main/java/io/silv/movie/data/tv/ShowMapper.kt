package io.silv.movie.data.tv


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

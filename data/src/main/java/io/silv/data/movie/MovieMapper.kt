package io.silv.data.movie

import io.silv.core.SGenre
import io.silv.core.SMovie
import io.silv.data.movie.model.Genre
import io.silv.data.movie.model.Movie

object MovieMapper {

    val mapMovie =
        { id: Long, title: String, posterUrl: String?, posterLastUpdated: Long, favorite: Boolean, externalUrl: String ->
            Movie(
                id = id,
                title = title,
                posterUrl  = posterUrl,
                favorite = favorite,
                posterLastUpdated = posterLastUpdated,
                externalUrl = externalUrl
            )
        }
}

fun SMovie.toDomain(): Movie {
    return Movie.create().copy(
        id = id,
        title = title,
        posterUrl = posterPath,
    )
}

fun SGenre.toDomain(): Genre {
    return Genre(name, id)
}

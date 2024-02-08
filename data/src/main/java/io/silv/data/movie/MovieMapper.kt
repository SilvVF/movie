package io.silv.data.movie

import io.silv.core.SMovie
import io.silv.data.movie.model.Movie

object MovieMapper {

    val mapMovie =
        { id: Long, title: String, posterUrl: String?, posterLastUpdated: Long, favorite: Boolean ->
            Movie(
                id = id,
                title = title,
                posterUrl  = posterUrl,
                favorite = favorite,
                posterLastUpdated = posterLastUpdated
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

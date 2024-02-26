package io.silv.movie.presentation


import io.silv.core_ui.components.PosterData
import io.silv.movie.data.movie.model.Movie
import io.silv.movie.data.tv.TVShow

fun Movie.toPoster(): PosterData {
    return PosterData(
        id = id,
        url = posterUrl,
        title = title,
        favorite = favorite
    )
}

fun TVShow.toPoster(): PosterData {
    return PosterData(
        id = id,
        url = posterUrl,
        title = title,
        favorite = favorite
    )
}

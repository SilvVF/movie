package io.silv.movie.presentation


import io.silv.core_ui.components.PosterData
import io.silv.movie.data.movie.model.Movie
import io.silv.movie.data.movie.model.MoviePoster
import io.silv.movie.data.tv.TVShow
import io.silv.movie.data.tv.TVShowPoster

fun MoviePoster.toPoster(): PosterData {
    return PosterData(
        id = id,
        url = posterUrl,
        title = title,
        favorite = favorite
    )
}


fun Movie.toPoster(): PosterData {
    return PosterData(
        id = id,
        url = posterUrl,
        title = title,
        favorite = favorite
    )
}

fun TVShowPoster.toPoster(): PosterData {
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

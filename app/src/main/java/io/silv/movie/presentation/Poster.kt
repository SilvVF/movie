package io.silv.movie.presentation


import io.silv.core_ui.components.PosterData
import io.silv.movie.data.movie.model.Movie
import io.silv.movie.data.movie.model.MoviePoster
import io.silv.movie.data.tv.model.TVShow
import io.silv.movie.data.tv.model.TVShowPoster

fun MoviePoster.toPoster(): PosterData {
    return PosterData(
        id = id,
        url = posterUrl,
        title = title,
        favorite = favorite,
        isMovie = true
    )
}


fun Movie.toPoster(): PosterData {
    return PosterData(
        id = id,
        url = posterUrl,
        title = title,
        favorite = favorite,
        isMovie = true
    )
}

fun TVShowPoster.toPoster(): PosterData {
    return PosterData(
        id = id,
        url = posterUrl,
        title = title,
        favorite = favorite,
        isMovie = false
    )
}

fun TVShow.toPoster(): PosterData {
    return PosterData(
        id = id,
        url = posterUrl,
        title = title,
        favorite = favorite,
        isMovie = false
    )
}

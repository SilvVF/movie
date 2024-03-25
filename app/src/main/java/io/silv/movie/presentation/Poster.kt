package io.silv.movie.presentation


import io.silv.core_ui.components.PosterData
import io.silv.movie.data.lists.ContentItem
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
        isMovie = true,
        inList = inLibraryLists >= 1L,
        lastModified = posterLastUpdated
    )
}


fun Movie.toPoster(): PosterData {
    return PosterData(
        id = id,
        url = posterUrl,
        title = title,
        favorite = favorite,
        isMovie = true,
        inList = inList,
        lastModified = posterLastUpdated
    )
}

fun TVShowPoster.toPoster(): PosterData {
    return PosterData(
        id = id,
        url = posterUrl,
        title = title,
        favorite = favorite,
        isMovie = false,
        inList = inLibraryLists >= 1L,
        lastModified = posterLastUpdated
    )
}

fun ContentItem.toPoster(): PosterData {
    return PosterData(
        id =  contentId,
        url = posterUrl,
        isMovie = isMovie,
        title =  title,
        favorite = favorite,
        lastModified = posterLastUpdated,
    )
}

fun TVShow.toPoster(): PosterData {
    return PosterData(
        id = id,
        url = posterUrl,
        title = title,
        favorite = favorite,
        isMovie = false,
        inList = inList,
        lastModified = posterLastUpdated
    )
}

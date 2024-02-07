package io.silv.data

import io.silv.core.SMovie
import io.silv.core_network.model.movie.MovieListResponse
import io.silv.core_network.model.movie.MovieSearchResponse

object MovieMapper {


}

fun SMovie.toDomain(): Movie {
    return Movie.create().copy(
        id = id,
        title = title,
        posterUrl = posterPath,
    )
}

fun MovieSearchResponse.Result.toSMovie(): SMovie {
    val m  = this
    return SMovie.create().apply {
        url = ""
        posterPath = "https://image.tmdb.org/t/p/original/${m.posterPath}"
        title = m.title
        genreIds = m.genreIds
        adult = m.adult
        releaseDate = m.releaseDate
        overview = m.overview
        id = m.id.toLong()
        originalTitle = m.originalTitle
        originalLanguage = m.originalLanguage
        title = m.title
        backdropPath = m.backdropPath
        popularity = m.popularity
        voteCount = m.voteCount
        video = m.video
        voteAverage = m.voteAverage
    }
}

fun MovieListResponse.Result.toSMovie(): SMovie {
    val m  = this
    return SMovie.create().apply {
        url = ""
        posterPath =  "https://image.tmdb.org/t/p/original/${m.posterPath}"
        title = m.title
        genreIds = m.genres.map { it.id }
        adult = m.adult
        releaseDate = m.releaseDate
        overview = m.overview
        genres = m.genres.map { Pair(it.id, it.name) }
        id = m.id.toLong()
        originalTitle = m.originalTitle
        originalLanguage = m.originalLanguage
        title = m.title
        backdropPath = m.backdropPath
        popularity = m.popularity
        voteCount = m.voteCount
        video = m.video
        voteAverage = m.voteAverage
    }
}
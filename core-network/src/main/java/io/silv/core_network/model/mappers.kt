package io.silv.core_network.model

import android.util.Log
import io.silv.core.SMovie
import io.silv.core.STVShow
import io.silv.core.STrailer
import io.silv.core_network.TMDBConstants
import io.silv.core_network.model.movie.MovieDiscoverResponse
import io.silv.core_network.model.movie.MovieListResponse
import io.silv.core_network.model.movie.MovieSearchResponse
import io.silv.core_network.model.movie.MovieVideoResponse
import io.silv.core_network.model.tv.TVResult

fun TVResult.toSTVShow(): STVShow {
    val m = this
    return STVShow.create().apply {
        url = "https://api.themoviedb.org/3/tv/${m.id}"
        posterPath = "https://image.tmdb.org/t/p/original${m.posterPath}".takeIf { m.posterPath != null }
        title = m.name
        genreIds = m.genreIds
        adult = m.adult
        releaseDate = m.firstAirDate
        overview = m.overview
        id = m.id.toLong()
        genres = m.genreIds.mapNotNull { id -> TMDBConstants.genreIdToName[id.toLong()]?.let { Pair(id, it) }  }
        originalTitle = m.originalName
        originalLanguage = m.originalLanguage
        backdropPath = m.backdropPath
        popularity = m.popularity
        voteCount = m.voteCount
        voteAverage = m.voteAverage
    }
}

fun MovieVideoResponse.Result.toSTrailer(): STrailer {
    val r = this
    return STrailer.create().apply {
        key = r.key
        trailerId = r.id
        name = r.name
        site = r.site
        size = r.size
        official = r.official
        publishedAt = r.publishedAt
        type = r.type
    }
}

fun MovieSearchResponse.Result.toSMovie(): SMovie {
    val m  = this
    return SMovie.create().apply {
        url = "https://api.themoviedb.org/3/movie/${m.id}"
        posterPath = "https://image.tmdb.org/t/p/original${m.posterPath}".takeIf { m.posterPath != null }
        title = m.title
        genreIds = m.genreIds
        adult = m.adult
        releaseDate = m.releaseDate
        overview = m.overview
        id = m.id.toLong()
        genres = m.genreIds.mapNotNull { it to (TMDBConstants.genreIdToName[it.toLong()] ?: return@mapNotNull null) }
        originalTitle = m.originalTitle
        originalLanguage = m.originalLanguage
        backdropPath = m.backdropPath
        popularity = m.popularity
        voteCount = m.voteCount
        video = m.video
        voteAverage = m.voteAverage
    }
}

fun MovieDiscoverResponse.Result.toSMovie(): SMovie {
    val m  = this
    return SMovie.create().apply {
        url = "https://api.themoviedb.org/3/movie/${m.id}"
        posterPath =  "https://image.tmdb.org/t/p/original${m.posterPath}".takeIf { m.posterPath.isNotBlank() }
        title = m.title
        genreIds = m.genreIds
        adult = m.adult
        releaseDate = m.releaseDate
        overview = m.overview
        genres = m.genreIds.mapNotNull { it to (TMDBConstants.genreIdToName[it.toLong()] ?: return@mapNotNull null) }
        id = m.id.toLong()
        originalTitle = m.originalTitle
        originalLanguage = m.originalLanguage
        backdropPath = m.backdropPath.orEmpty()
        popularity = m.popularity
        voteCount = m.voteCount
        video = m.video
        voteAverage = m.voteAverage
    }
}

fun MovieListResponse.Result.toSMovie(): SMovie {
    val m  = this
    Log.d("movie", m.toString())
    return SMovie.create().apply {
        url = "https://api.themoviedb.org/3/movie/${m.id}"
        posterPath =  "https://image.tmdb.org/t/p/original${m.posterPath}".takeIf { m.posterPath != null }
        title = m.title
        genreIds = m.genreIds
        adult = m.adult
        releaseDate = m.releaseDate
        overview = m.overview
        genres = genreIds?.mapNotNull { it to (TMDBConstants.genreIdToName[it.toLong()] ?: return@mapNotNull null) }
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
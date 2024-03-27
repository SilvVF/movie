package io.silv.movie.network.model

import io.silv.movie.core.SCredit
import io.silv.movie.core.SMovie
import io.silv.movie.core.STVShow
import io.silv.movie.network.model.credits.CreditsResponse
import io.silv.movie.network.model.movie.MovieDiscoverResponse
import io.silv.movie.network.model.movie.MovieListResponse
import io.silv.movie.network.model.movie.MovieSearchResponse
import io.silv.movie.network.model.movie.MovieVideoResponse
import io.silv.movie.network.model.recommendation.MovieRecommendationResponse
import io.silv.movie.network.model.recommendation.TVSeriesRecommendationResponse
import io.silv.movie.network.model.tv.TVResult
import io.silv.movie.network.model.tv.TVVideoResponse
import io.silv.movie.network.service.tmdb.TMDBConstants

fun CreditsResponse.toSCredits(): List<SCredit>  {
    return cast.map {
        SCredit.create().apply {
            this.id = it.id
            this.adult = it.adult
            this.gender = it.gender
            this.knownForDepartment = it.knownForDepartment
            this.name = it.name
            this.originalName = it.originalName
            this.popularity = it.popularity
            this.profilePath = if (it.profilePath != null)
                "https://image.tmdb.org/t/p/original${it.profilePath}"
            else null
            this.character = it.character
            this.creditId = it.creditId
            this.order = it.order.toLong()
            this.crew = false
        }
    } + crew.map {
        SCredit.create().apply {
            this.id = it.id
            this.adult = it.adult
            this.gender = it.gender
            this.knownForDepartment = it.knownForDepartment
            this.name = it.name
            this.originalName = it.originalName
            this.popularity = it.popularity
            this.profilePath = if (it.profilePath != null)
                "https://image.tmdb.org/t/p/original${it.profilePath}"
            else null
            this.character = it.job
            this.creditId = it.creditId
            this.crew = true
        }
    }
}

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

fun MovieRecommendationResponse.Result.toSMovie(): SMovie {
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

fun TVSeriesRecommendationResponse.Result.toSTVShow(): STVShow {
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

fun MovieVideoResponse.Result.toSTrailer(): io.silv.movie.core.STrailer {
    val r = this
    return io.silv.movie.core.STrailer.create().apply {
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

fun TVVideoResponse.Result.toSTrailer(): io.silv.movie.core.STrailer {
    val r = this
    return io.silv.movie.core.STrailer.create().apply {
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
        posterPath =  "https://image.tmdb.org/t/p/original${m.posterPath}".takeIf { m.posterPath.orEmpty().isNotBlank() }
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
package io.silv.data.movie

import io.silv.core.SGenre
import io.silv.core.SMovie
import io.silv.core.STVShow
import io.silv.data.movie.model.Genre
import io.silv.data.movie.model.Movie
import io.silv.data.movie.model.TVShow

object MovieMapper {

    val mapMovie =
        { id: Long, title: String, overview: String,genres: List<String>?, genreIds: List<Int>?,originalLanguage: String,voteCount: Long,releaseDate: String,posterUrl: String?, posterLastUpdated: Long,favorite: Boolean, externalUrl: String, popularity: Double ->
            Movie(
                id = id,
                title = title,
                posterUrl  = posterUrl,
                favorite = favorite,
                posterLastUpdated = posterLastUpdated,
                externalUrl = externalUrl,
                overview = overview,
                genres = genres ?: emptyList(),
                genreIds = genreIds ?: emptyList(),
                originalLanguage = originalLanguage,
                popularity = popularity,
                voteCount = voteCount.toInt(),
                releaseDate = releaseDate
            )
        }
}

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

fun SMovie.toDomain(): Movie {
    return Movie.create().copy(
        id = id,
        title = title,
        posterUrl = posterPath,
        overview = overview,
        genres =  genres?.map { it.second } ?: emptyList(),
        genreIds = genreIds ?: genres?.map { it.first } ?: emptyList(),
        originalLanguage = originalLanguage,
        popularity = popularity,
        voteCount = voteCount,
        releaseDate = releaseDate,
        externalUrl = url,
    )
}

fun STVShow.toDomain(): TVShow {
    return TVShow.create().copy(
        id = id,
        title = title,
        posterUrl = posterPath,
    )
}

fun SGenre.toDomain(): Genre {
    return Genre(name, id)
}

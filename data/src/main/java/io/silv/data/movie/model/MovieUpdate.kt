package io.silv.data.movie.model

import io.silv.core.Status

data class MovieUpdate(
    val movieId: Long,
    val title: String?,
    val overview: String?,
    val genres: List<String>?,
    val genreIds: List<Int>?,
    val originalLanguage: String?,
    val popularity: Double?,
    val voteCount: Int?,
    val releaseDate: String?,
    val externalUrl: String?,
    val posterUrl: String?,
    val posterLastUpdated: Long?,
    val favorite: Boolean?,
    val status: Status?
)

fun Movie.toMovieUpdate(): MovieUpdate {
    return MovieUpdate(
        movieId = id,
        favorite = favorite,
        title = title,
        externalUrl = externalUrl,
        posterUrl = posterUrl,
        posterLastUpdated = posterLastUpdated,
        overview = overview,
        genres = genres,
        genreIds = genreIds,
        originalLanguage = originalLanguage,
        popularity = popularity,
        voteCount = voteCount,
        releaseDate = releaseDate,
        status = status
    )
}

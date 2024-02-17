package io.silv.data.movie.model

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
    val favorite: Boolean?
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
        releaseDate = releaseDate
    )
}

data class ShowUpdate(
    val showId: Long,
    val title: String? = null,
    val externalUrl: String? = null,
    val posterUrl: String? = null,
    val posterLastUpdated: Long? = null,
    val favorite: Boolean? = null
)

fun TVShow.toShowUpdate(): ShowUpdate {
    return ShowUpdate(
        showId = id,
        favorite = favorite,
        title = title,
        externalUrl = externalUrl,
        posterUrl = posterUrl,
        posterLastUpdated = posterLastUpdated
    )
}
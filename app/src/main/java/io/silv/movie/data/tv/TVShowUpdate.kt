package io.silv.movie.data.tv

data class TVShowUpdate(
    val showId: Long,
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
    val status: io.silv.movie.core.Status?
)

fun TVShow.toShowUpdate(): TVShowUpdate {
    return TVShowUpdate(
        showId = id,
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
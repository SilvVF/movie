package io.silv.movie.data.model

data class TVShowUpdate(
    val showId: Long,
    val title: String? = null,
    val overview: String? = null,
    val genres: List<String>? = null,
    val genreIds: List<Int>? = null,
    val originalLanguage: String? = null,
    val popularity: Double? = null,
    val voteCount: Int? = null,
    val releaseDate: String? = null,
    val externalUrl: String? = null,
    val posterUrl: String? = null,
    val posterLastUpdated: Long? = null,
    val favorite: Boolean? = null,
    val status: io.silv.movie.core.Status? = null,
    val productionCompanies: List<String>? = null
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
        status = status,
        productionCompanies = productionCompanies,
    )
}
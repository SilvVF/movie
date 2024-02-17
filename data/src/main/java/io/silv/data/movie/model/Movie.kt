package io.silv.data.movie.model

import androidx.compose.runtime.Stable

@Stable
data class Movie(
    val id: Long,
    val title: String,
    val overview: String,
    val genres: List<String>,
    val genreIds: List<Int>,
    val originalLanguage: String,
    val popularity: Double,
    val voteCount: Int,
    val releaseDate: String,
    val externalUrl: String,
    val posterUrl: String?,
    val posterLastUpdated: Long,
    val favorite: Boolean
) {

    companion object {
        fun create() = Movie(
            id = -1L,
            posterUrl = "",
            title = "",
            favorite = false,
            posterLastUpdated = -1L,
            externalUrl = "",
            overview = "",
            genres = emptyList(),
            originalLanguage = "",
            popularity = 0.0,
            voteCount = 0,
            releaseDate = "",
            genreIds = emptyList()
       )
    }
}


@Stable
data class TVShow(
    val id: Long,
    val title: String,
    val externalUrl: String,
    val posterUrl: String?,
    val posterLastUpdated: Long,
    val favorite: Boolean
) {

    companion object {
        fun create() = TVShow(
            id = -1L,
            posterUrl = "",
            title = "",
            favorite = false,
            posterLastUpdated = -1L,
            externalUrl = ""
        )
    }
}


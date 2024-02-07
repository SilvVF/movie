package io.silv.core_network.model.movie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class MovieListResponse(
    @SerialName("page")
    val page: Int = 0,
    @SerialName("results")
    val results: List<Result> = emptyList(),
    @SerialName("total_pages")
    val totalPages: Int = 0,
    @SerialName("total_results")
    val totalResults: Int = 0,
    @SerialName("dates")
    val dates: Dates? = null
) {
    /**
     * Only available when calling upcoming and now_playing
     */
    @Serializable
    data class Dates(
        @SerialName("maximum")
        val maximum: String = "",
        @SerialName("minimum")
        val minimum: String = ""
    )

    @Serializable
    data class Result(
        @SerialName("poster_path")
        val posterPath: String? = null,
        @SerialName("adult")
        val adult: Boolean = false,
        @SerialName("release_date")
        val releaseDate: String = "",
        @SerialName("overview")
        val overview: String = "",
        @SerialName("genres")
        val genres: List<Genres> = emptyList(),
        @SerialName("id")
        val id: Int = 0,
        @SerialName("original_language")
        val originalLanguage: String = "",
        @SerialName("original_title")
        val originalTitle: String = "",
        @SerialName("title")
        val title: String = "",
        @SerialName("backdrop_path")
        val backdropPath: String? = "",
        @SerialName("popularity")
        val popularity: Double = 0.0,
        @SerialName("vote_count")
        val voteCount: Int = 0,
        @SerialName("video")
        val video: Boolean = false,
        @SerialName("vote_average")
        val voteAverage: Double = 0.0,
    )

    @Serializable
    data class Genres(
        @SerialName("id")
        val id: Int,

        @SerialName("name")
        val name: String
    )
}

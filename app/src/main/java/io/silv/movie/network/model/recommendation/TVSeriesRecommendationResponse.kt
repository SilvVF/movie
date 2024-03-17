package io.silv.movie.network.model.recommendation


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TVSeriesRecommendationResponse(
    @SerialName("page")
    val page: Int = 0,
    @SerialName("results")
    val results: List<Result> = listOf(),
    @SerialName("total_pages")
    val totalPages: Int = 0,
    @SerialName("total_results")
    val totalResults: Int = 0
) {
    @Serializable
    data class Result(
        @SerialName("adult")
        val adult: Boolean = false,
        @SerialName("backdrop_path")
        val backdropPath: String? = null,
        @SerialName("first_air_date")
        val firstAirDate: String = "",
        @SerialName("genre_ids")
        val genreIds: List<Int> = listOf(),
        @SerialName("id")
        val id: Int = 0,
        @SerialName("media_type")
        val mediaType: String = "",
        @SerialName("name")
        val name: String = "",
        @SerialName("origin_country")
        val originCountry: List<String> = listOf(),
        @SerialName("original_language")
        val originalLanguage: String = "",
        @SerialName("original_name")
        val originalName: String = "",
        @SerialName("overview")
        val overview: String = "",
        @SerialName("popularity")
        val popularity: Double = 0.0,
        @SerialName("poster_path")
        val posterPath: String? = null,
        @SerialName("vote_average")
        val voteAverage: Double = 0.0,
        @SerialName("vote_count")
        val voteCount: Int = 0
    )
}
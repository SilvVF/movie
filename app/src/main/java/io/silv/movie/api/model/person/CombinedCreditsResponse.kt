package io.silv.movie.api.model.person


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CombinedCreditsResponse(
    @SerialName("cast")
    val cast: List<Cast> = listOf(),
    @SerialName("crew")
    val crew: List<Crew> = listOf(),
    @SerialName("id")
    val id: Int = 0
) {
    @Serializable
    data class Cast(
        @SerialName("adult")
        val adult: Boolean = false,
        @SerialName("backdrop_path")
        val backdropPath: String? = "",
        @SerialName("character")
        val character: String = "",
        @SerialName("credit_id")
        val creditId: String = "",
        @SerialName("episode_count")
        val episodeCount: Int? = 0,
        @SerialName("first_air_date")
        val firstAirDate: String? = "",
        @SerialName("genre_ids")
        val genreIds: List<Int> = listOf(),
        @SerialName("id")
        val id: Int = 0,
        @SerialName("media_type")
        val mediaType: String = "",
        @SerialName("name")
        val name: String? = "",
        @SerialName("order")
        val order: Int? = 0,
        @SerialName("origin_country")
        val originCountry: List<String>? = listOf(),
        @SerialName("original_language")
        val originalLanguage: String = "",
        @SerialName("original_name")
        val originalName: String? = "",
        @SerialName("original_title")
        val originalTitle: String? = "",
        @SerialName("overview")
        val overview: String = "",
        @SerialName("popularity")
        val popularity: Double = 0.0,
        @SerialName("poster_path")
        val posterPath: String? = "",
        @SerialName("release_date")
        val releaseDate: String? = "",
        @SerialName("title")
        val title: String? = "",
        @SerialName("video")
        val video: Boolean? = false,
        @SerialName("vote_average")
        val voteAverage: Double = 0.0,
        @SerialName("vote_count")
        val voteCount: Int = 0
    )

    @Serializable
    data class Crew(
        @SerialName("adult")
        val adult: Boolean = false,
        @SerialName("backdrop_path")
        val backdropPath: String? = "",
        @SerialName("credit_id")
        val creditId: String = "",
        @SerialName("department")
        val department: String = "",
        @SerialName("genre_ids")
        val genreIds: List<Int> = listOf(),
        @SerialName("id")
        val id: Int = 0,
        @SerialName("job")
        val job: String = "",
        @SerialName("media_type")
        val mediaType: String = "",
        @SerialName("original_language")
        val originalLanguage: String = "",
        @SerialName("original_title")
        val originalTitle: String = "",
        @SerialName("overview")
        val overview: String = "",
        @SerialName("popularity")
        val popularity: Double = 0.0,
        @SerialName("poster_path")
        val posterPath: String? = "",
        @SerialName("release_date")
        val releaseDate: String = "",
        @SerialName("title")
        val title: String = "",
        @SerialName("video")
        val video: Boolean = false,
        @SerialName("vote_average")
        val voteAverage: Double = 0.0,
        @SerialName("vote_count")
        val voteCount: Int = 0
    )
}
package io.silv.core_network

import io.silv.core_network.model.movie.MovieDiscoverResponse
import io.silv.core_network.model.movie.MovieListResponse
import io.silv.core_network.model.movie.MovieSearchResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface TMDBMovieService {

    @GET("movie/{type}")
    fun movieList(
        @Path("type") type: String,
        @Query("language") language: String? = "en-US",
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null
    ): Call<MovieListResponse>

    @GET("search/movie")
    fun search(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean? = false,
        @Query("language") language: String? = "en-US",
        @Query("primary_release_year") primaryReleaseYear: String? = null,
        @Query("region") region: String? = null,
        @Query("year") year: String? = null
    ): Call<MovieSearchResponse>

    @GET("discover/movie")
    fun discover(
        @Query("page") page: Int,
        @Query("with_genres") genres: String? = null,
        @Query("certification") certification: String? = null,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("include_video") includeVideo: Boolean = false,
        @Query("language") language: String? = "en-US",
        @Query("certification.gte") certificationGte: String? = null,
        @Query("certification.lte") certificationLte: String? = null,
        @Query("certification_country") certificationCountry: String? = null,
        @Query("primary_release_year") primaryReleaseYear: Int? = null,
        @Query("primary_release_date.gte") primaryReleaseDateGte: String? = null,
        @Query("primary_release_date.lte") primaryReleaseDateLte: String? = null
    ): Call<MovieDiscoverResponse>

    enum class MovieType {
        Popular { override fun toString(): String { return "popular" } },
        NowPlaying { override fun toString(): String { return "now_playing" }},
        TopRated { override fun toString(): String { return "top_rated" }},
        Upcoming { override fun toString(): String { return "upcoming" }},
    }

    companion object{

        private fun genresStringFromIds(genreIds: List<Long>, joinMode: Int): String {
            return when {
                joinMode and JOIN_MODE_MASK_OR == JOIN_MODE_MASK_OR -> {
                    genreIds.joinToString(separator = "|")
                }
                joinMode and JOIN_MODE_MASK_AND == JOIN_MODE_MASK_AND -> {
                    genreIds.joinToString(",")
                }
                else -> ""
            }
        }

        fun genresString(genres: List<String>, joinMode: Int): String {
            val genreIds = genres.mapNotNull { TMDBConstants.genreNameToId[it] }
            return genresStringFromIds(genreIds, joinMode)
        }

        const val JOIN_MODE_MASK_OR = 0x1
        const val JOIN_MODE_MASK_AND = 0x10
    }
}
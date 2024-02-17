package io.silv.core_network

import io.silv.core_network.model.movie.MovieDetailsResponse
import io.silv.core_network.model.movie.MovieDiscoverResponse
import io.silv.core_network.model.movie.MovieListResponse
import io.silv.core_network.model.movie.MovieSearchResponse
import io.silv.core_network.model.movie.MovieVideoResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface TMDBMovieService {

    @GET("https://api.themoviedb.org/3/movie/{id}")
    fun details(
        @Path("id") id: Long,
    ): Call<MovieDetailsResponse>

    @GET("movie/{type}")
    fun movieList(
        @Path("type") type: String,
        @Query("language") language: String? = "en-US",
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null
    ): Call<MovieListResponse>

    @GET("movie/{movie_id}/videos")
    fun videos(
        @Path("movie_id") id: Long,
        @Query("language") language: String = "en-US"
    ): Call<MovieVideoResponse>

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
}
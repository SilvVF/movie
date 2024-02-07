package io.silv.core_network

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

    enum class MovieType {
        Popular { override fun toString(): String { return "popular" } },
        NowPlaying { override fun toString(): String { return "now_playing" }},
        TopRated { override fun toString(): String { return "top_rated" }},
        Upcoming { override fun toString(): String { return "upcoming" }},
    }
}
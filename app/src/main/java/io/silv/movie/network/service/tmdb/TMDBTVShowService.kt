package io.silv.movie.network.service.tmdb

import io.silv.movie.network.model.credits.CreditsResponse
import io.silv.movie.network.model.tv.TVDetailsResponse
import io.silv.movie.network.model.tv.TVDiscoverResponse
import io.silv.movie.network.model.tv.TVListResponse
import io.silv.movie.network.model.tv.TVSearchResponse
import io.silv.movie.network.model.tv.TVVideoResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface TMDBTVShowService {

    @GET("tv/{series_id}/videos")
    fun videos(
        @Path("series_id") id: Long,
        @Query("language") language: String = "en-US"
    ): Call<TVVideoResponse>

    @GET("tv/{id}")
    fun details(
        @Path("id") id: Long,
    ): Call<TVDetailsResponse>

    @GET("{series_id}/credits")
    fun credits(
        @Path("series_id") id: Long
    ): Call<CreditsResponse>


    @GET("tv/{type}")
    fun tvList(
        @Path("type") type: String,
        @Query("language") language: String? = "en-US",
        @Query("page") page: Int = 1,
        @Query("timezone") timeZone: String? = null
    ): Call<TVListResponse>

    @GET("search/tv")
    fun search(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean? = false,
        @Query("language") language: String? = "en-US",
        @Query("year") year: String? = null
    ): Call<TVSearchResponse>

    @GET("discover/tv")
    fun discover(
        @Query("page") page: Int,
        @Query("first_air_date_year") year: Int? = null,
        @Query("with_genres") genres: String? = null,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String? = "en-US",
        @Query("vote_average") voteAverage: Float? = null,
        @Query("vote_count") voteCount: Float? = null,
        @Query("with_keywords") keywords: String? = null,
        @Query("with_people") people: String? = null,
        @Query("with_companies") companies: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
    ): Call<TVDiscoverResponse>

    enum class TVType {
        Popular { override fun toString(): String { return "popular" } },
        NowPlaying { override fun toString(): String { return "airing_today" }},
        TopRated { override fun toString(): String { return "top_rated" }},
        Upcoming { override fun toString(): String { return "on_the_air" }},
    }
}
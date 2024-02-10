package io.silv.core_network

import io.silv.core_network.model.tv.TVDetailsResponse
import io.silv.core_network.model.tv.TVDiscoverResponse
import io.silv.core_network.model.tv.TVListResponse
import io.silv.core_network.model.tv.TVSearchResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface TMDBTVShowService {

    @GET("https://api.themoviedb.org/3/tv/{id}")
    fun details(
        @Path("id") id: Long,
    ): Call<TVDetailsResponse>

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
        @Query("with_genres") genres: String? = null,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String? = "en-US",
    ): Call<TVDiscoverResponse>

    enum class TVType {
        Popular { override fun toString(): String { return "popular" } },
        NowPlaying { override fun toString(): String { return "airing_today" }},
        TopRated { override fun toString(): String { return "top_rated" }},
        Upcoming { override fun toString(): String { return "on_the_air" }},
    }
}
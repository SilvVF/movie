package io.silv.movie.api.service.tmdb

import io.silv.movie.api.model.recommendation.MovieRecommendationResponse
import io.silv.movie.api.model.recommendation.TVSeriesRecommendationResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TMDBRecommendationService {

    @GET("movie/{movie_id}/recommendations")
    fun movieRecommendations(
        @Path("movie_id") id: Int,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): Call<MovieRecommendationResponse>

    @GET("tv/{series_id}/recommendations")
    fun showRecommendations(
        @Path("series_id") id: Int,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): Call<TVSeriesRecommendationResponse>
}
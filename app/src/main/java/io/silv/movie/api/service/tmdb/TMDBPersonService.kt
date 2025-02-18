package io.silv.movie.api.service.tmdb

import io.silv.movie.api.model.person.CombinedCreditsResponse
import io.silv.movie.api.model.person.PersonDetailsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface TMDBPersonService {

    @GET("person/{person_id}/combined_credits")
    fun combinedCredits(
        @Path("person_id") id: String,
    ): Call<CombinedCreditsResponse>

    @GET("person/{person_id}")
    fun details(
        @Path("person_id") id: String,
    ): Call<PersonDetailsResponse>
}
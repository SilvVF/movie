package io.silv.movie.network.service.piped

import io.silv.movie.network.model.Streams
import retrofit2.http.GET
import retrofit2.http.Path

interface PipedApi {

    @GET("streams/{videoId}")
    suspend fun getStreams(@Path("videoId") videoId: String): Streams
}


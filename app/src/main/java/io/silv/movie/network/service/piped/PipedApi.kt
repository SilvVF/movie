package io.silv.movie.network.service.piped

import io.silv.movie.core.await
import io.silv.movie.network.GET
import io.silv.movie.network.model.Streams
import io.silv.movie.network.parseAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class PipedApi(
    private val client: OkHttpClient,
    private val json: Json
) {

    suspend fun getStreams(videoId: String): Streams = withContext(Dispatchers.IO){
        client.newCall(
            GET("https://pipedapi.adminforge.de/streams/$videoId")
        )
            .await()
            .parseAs<Streams>(json)
    }
}


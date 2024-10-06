package io.silv.movie.network.service.piped

import io.silv.movie.core.await
import io.silv.movie.network.GET
import io.silv.movie.network.model.Streams
import io.silv.movie.network.parseAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Headers
import timber.log.Timber

class PipedApi(
    private val client: OkHttpClient,
    private val json: Json
) {
//    val urlList by lazy { runBlocking {
//            val md = client.newCall(
//                GET(
//                "https://raw.githubusercontent.com/wiki/TeamPiped/Piped-Frontend/Instances.md"
//                )
//            )
//                .await()
//        }
//    }

    suspend fun getStreams(videoId: String): Streams = withContext(Dispatchers.IO){
        Timber.d("https://api.piped.yt/streams/$videoId")

        client.newCall(
            GET(
                url = "https://api.piped.yt/streams/$videoId",
                headers = Headers.Builder()
                    .add("Accept", "application/json")
                    .build()
            )
        )
            .await()
            .parseAs<Streams>(json)
    }
}


package io.silv.movie.api.service.piped

import io.silv.movie.IoDispatcher
import io.silv.movie.core.await
import io.silv.movie.api.GET
import io.silv.movie.api.model.Streams
import io.silv.movie.api.parseAs
import io.silv.movie.core.pmapSupervised
import io.silv.movie.core.suspendRunCatching
import io.silv.movie.extract.dohCloudflare
import io.silv.movie.prefrences.BasePreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Headers
import timber.log.Timber

data class PipedProvider(
    val name: String,
    val url: String,
    val languages: List<String>,
    val cdn: Boolean,
    val users: Int
)

class PipedApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val basePreferences: BasePreferences,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher
) {
    private val pipedUrl = basePreferences.pipedUrl()

    suspend fun getUrlList(): Result<List<PipedProvider>> = withContext(ioDispatcher) {
        suspendRunCatching {
            val doc = client.newCall(
                GET(
                    "https://raw.githubusercontent.com/wiki/TeamPiped/Piped-Frontend/Instances.md"
                )
            )
                .await()
                .body?.string()!!

            val lines = doc.lines()
            val table = lines.indexOfFirst { it.contains("--- | --- | --- | --- | ---") }

            lines.slice(table + 1..lines.lastIndex).map {
                val split = it.split("|")
                PipedProvider(
                    name = split[0],
                    url = split[1],
                    languages = split[2].split(","),
                    cdn = split[3] == "Yes",
                    users = 0/*getUserCount(
                        split[4].trim()
                            .removePrefix("![](")
                            .removeSuffix(")")
                    ).getOrDefault(0)*/
                )
            }
        }
    }

//    private val userClient by lazy {
//        client
//            .newBuilder()
//            .dohCloudflare()
//            .build()
//    }
//
//    private suspend fun getUserCount(url: String): Result<Int> {
//        return suspendRunCatching {
//            withTimeout(5000) {
//                Timber.d(url)
//                val body = userClient
//                    .newCall(GET(url)).await().body?.string()!!
//                Timber.d(body)
//                val t = "Registered Users: "
//                val idx = body.indexOf(t) + t.length
//                body.slice(idx..body.length)
//                    .takeWhile { c -> c.isDigit() }
//                    .toInt()
//            }
//        }
//            .onFailure { Timber.e(it) }
//    }

    suspend fun getStreams(videoId: String): Result<Streams> = withContext(ioDispatcher) {
        suspendRunCatching {
            Timber.d("${pipedUrl.get().trim()}/streams/$videoId")
            client.newCall(
                GET(
                    url = "${pipedUrl.get().trim()}/streams/$videoId",
                    headers = Headers.Builder()
                        .add("Accept", "application/json")
                        .build()
                )
            )
                .await()
                .parseAs<Streams>(json)
        }
    }
}


package io.silv.movie.extract

import android.util.Base64
import io.silv.movie.network.GET
import io.silv.movie.network.parseAs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class VidsrcExtractor(private val client: OkHttpClient, private val headers: Headers):
    KoinComponent {

    private val playlistUtils by lazy { PlaylistUtils(client, headers) }
    private val json: Json by inject()

    fun videosFromUrl(
        embedLink: String,
        hosterName: String,
        type: String = "",
        subtitleList: List<Track> = emptyList(),
    ): List<Video> {
        val host = embedLink.toHttpUrl().host
        val apiUrl = getApiUrl(embedLink)

        val response = client.newCall(GET(apiUrl)).execute()
        val data = response.parseAs<MediaResponseBody>(json)

        val decrypted = vrfDecrypt(data.result)
        val result = json.decodeFromString<MediaResponseBody.Result>(decrypted)

        return playlistUtils.extractFromHls(
            playlistUrl = result.sources.first().file,
            referer = "https://$host/",
            videoNameGen = { q -> hosterName + (if (type.isBlank()) "" else " - $type") + " - $q" },
            subtitleList = subtitleList + result.tracks.toTracks(),
        )
    }

    private fun getApiUrl(embedLink: String): String {
        val host = embedLink.toHttpUrl().host
        val params = embedLink.toHttpUrl().let { url ->
            url.queryParameterNames.map {
                Pair(it, url.queryParameter(it) ?: "")
            }
        }
        val vidId = embedLink.substringAfterLast("/").substringBefore("?")
        val apiSlug = encodeID(vidId, ENCRYPTION_KEY1)
        val h = encodeID(vidId, ENCRYPTION_KEY2)

        return buildString {
            append("https://")
            append(host)
            append("/mediainfo/")
            append(apiSlug)
            if (params.isNotEmpty()) {
                append("?")
                append(
                    params.joinToString("&") {
                        "${it.first}=${it.second}"
                    },
                )
                append("&h=$h")
            }
        }
    }

    private fun encodeID(videoID: String, key: String): String {
        val rc4Key = SecretKeySpec(key.toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
        return Base64.encode(cipher.doFinal(videoID.toByteArray()), Base64.DEFAULT)
            .toString(Charsets.UTF_8)
            .replace("+", "-")
            .replace("/", "_")
            .trim()
    }

    private fun List<MediaResponseBody.Result.SubTrack>.toTracks(): List<Track> {
        return filter {
            it.kind == "captions"
        }.mapNotNull {
            runCatching {
                Track(
                    it.file,
                    it.label,
                )
            }.getOrNull()
        }
    }

    private fun vrfDecrypt(input: String): String {
        var vrf = Base64.decode(input.toByteArray(), Base64.URL_SAFE)
        val rc4Key = SecretKeySpec(DECRYPTION_KEY.toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
        vrf = cipher.doFinal(vrf)
        return URLDecoder.decode(vrf.toString(Charsets.UTF_8), "utf-8")
    }

    companion object {
        private const val ENCRYPTION_KEY1 = "8Qy3mlM2kod80XIK"
        private const val ENCRYPTION_KEY2 = "BgKVSrzpH2Enosgm"
        private const val DECRYPTION_KEY = "9jXDYBZUcTcTZveM"
    }
}

@Serializable
data class MediaResponseBody(
    val status: Int,
    val result: String,
) {
    @Serializable
    data class Result(
        val sources: List<Source>,
        val tracks: List<SubTrack> = emptyList(),
    ) {
        @Serializable
        data class Source(
            val file: String,
        )

        @Serializable
        data class SubTrack(
            val file: String,
            val label: String = "",
            val kind: String,
        )
    }
}
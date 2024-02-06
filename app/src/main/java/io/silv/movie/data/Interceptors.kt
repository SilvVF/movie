package io.silv.movie.data

import android.util.Log
import io.silv.movie.BuildConfig
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlinx.datetime.toInstant
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AuthInterceptor: Interceptor {

    private val accessToken = BuildConfig.TMDB_ACCESS_TOKEN

    override fun intercept(chain: Interceptor.Chain): Response {
        val newReq = chain.request()
            .newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        return chain.proceed(newReq)
    }
}

class SessionInterceptor(
    private val json: Json,
): Interceptor {


    private val client: OkHttpClient = OkHttpClient()
        .newBuilder()
        .build()

    private var currentSession: TMDBGuestSession? = null
        set(value) {
            Log.d("SessionInterceptor", value.toString())
            field = value
        }

    private fun getSession(): TMDBGuestSession {
        val request = Request.Builder()
            .url("https://api.themoviedb.org/3/authentication/guest_session/new")
            .get()
            .addHeader("Accept", "application/json")
            .build()

        val resp = client.newCall(request).execute().body!!.string()

        return json.decodeFromString<TMDBGuestSession>(resp)
    }

    override fun intercept(chain: Interceptor.Chain): Response {

        if (true) {
            return chain.proceed(chain.request())
        }

        val session = currentSession?.takeIf {
            val timestamp = it.expiresAt
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")

            val dateTime = LocalDateTime.parse(timestamp, formatter)
                .toKotlinLocalDateTime()
                .toInstant(TimeZone.UTC)

            dateTime.periodUntil(Clock.System.now(), TimeZone.UTC).minutes < 50
        }
            ?: getSession()
        val newReq = chain.request()
            .newBuilder()
            .build()


        return chain.proceed(newReq)
    }

    @Serializable
    private data class TMDBGuestSession(
        val success: Boolean,
        @SerialName("guest_session_id")
        val guestSessionId: String,
        @SerialName("expires_at")
        val expiresAt: String,
    )
}

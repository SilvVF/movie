package io.silv.core_network.ratelimit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * An OkHttp interceptor that handles rate limiting.
 *
 * Examples:
 *
 * permits = 5, period = 1, unit = seconds => 5 requests per second permits = 10, period = 2, unit =
 * minutes => 10 requests per 2 minutes
 *
 * @param permits {Int} Number of requests allowed within a period of units.
 * @param period {Long} The limiting duration. Defaults to 1.
 */
fun OkHttpClient.Builder.rateLimit(
    permits: Int,
    period: Duration = 1.seconds
) = addInterceptor(
    RateLimitInterceptor(
        null,
        permits.toLong(),
        period.inWholeNanoseconds
    )
)

private class RateLimitInterceptor(
    private val host: String?,
    permits: Long,
    nanoSeconds: Long,
) : Interceptor {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val bucket =
        TokenBuckets.builder()
            .withCapacity(permits)
            .withFixedIntervalRefillStrategy(permits, nanoSeconds)
            .build()

    override fun intercept(chain: Interceptor.Chain): Response {
        if (chain.call().isCanceled()) {
            throw IOException()
        }

        val request = chain.request()

        when (host) {
            null, request.url.host -> {} // need rate limit
            else -> return chain.proceed(request)
        }

        runBlocking { bucket.consume() }

        if (chain.call().isCanceled()) {
            throw IOException()
        }

        val response = chain.proceed(chain.request())

        // if the response was from the cache add token back
        if (response.networkResponse == null) {
            scope.launch {
                bucket.refill(1)
            }
        }
        return response
    }
}
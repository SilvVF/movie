package io.silv.movie.api.service.tmdb

import io.silv.movie.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class TMDBAuthInterceptor: Interceptor {

    private val accessToken = BuildConfig.TMDB_ACCESS_TOKEN

    override fun intercept(chain: Interceptor.Chain): Response {

        val newReq = chain.request()
            .newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        return chain.proceed(newReq)
    }
}
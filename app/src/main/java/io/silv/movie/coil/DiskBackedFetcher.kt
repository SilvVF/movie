package io.silv.movie.coil

import android.content.Context
import coil.fetch.FetchResult
import coil.key.Keyer
import coil.request.Options
import okhttp3.Response

interface FetcherConfig<T: Any> {
    val keyer: Keyer<T>
    val diskStore: FetcherDiskStore<T>
    val context: Context
    suspend fun overrideFetch(options: Options, data: T): FetchResult? { return null }
}

interface ByteArrayFetcherConfig<T : Any>: FetcherConfig<T> {
    suspend fun fetch(options: Options, data: T): ByteArray
}

interface OkHttpFetcherConfig<T : Any>: FetcherConfig<T> {
    suspend fun fetch(options: Options, data: T): Response
}

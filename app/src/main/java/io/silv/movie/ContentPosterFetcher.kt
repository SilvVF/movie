package io.silv.movie

import android.content.Context
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import coil.request.Parameters
import io.silv.core_ui.components.PosterData
import io.silv.movie.coil.FetcherDiskStore
import io.silv.movie.coil.FetcherDiskStoreImageFile
import io.silv.movie.coil.OkHttpFetcherConfig
import io.silv.movie.core.await
import io.silv.movie.data.cache.MovieCoverCache
import io.silv.movie.data.cache.TVShowCoverCache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Path.Companion.toOkioPath

class ContentPosterFetcher(
    override val context: Context,
    private val client: OkHttpClient,
    private val movieCoverCache: MovieCoverCache,
    private val tvShowCoverCache: TVShowCoverCache
): OkHttpFetcherConfig<PosterData> {

    override val keyer: Keyer<PosterData> =
        Keyer { data, options ->
            val hasCustomCover = if (data.isMovie) {
                movieCoverCache.getCustomCoverFile(data.id).exists()
            } else {
                tvShowCoverCache.getCustomCoverFile(data.id).exists()
            }
            val prefix = if(data.isMovie) "movie" else "show"

            if (hasCustomCover) {
                "$prefix;${data.id};${data.lastModified}"
            } else {
                "$prefix;${data.url};${data.lastModified}"
            }
        }

    override val diskStore: FetcherDiskStore<PosterData> =
        FetcherDiskStoreImageFile { data, _ ->
            if (data.favorite) {
                if (data.isMovie) {
                    movieCoverCache.getCoverFile(data.url)
                } else {
                    tvShowCoverCache.getCoverFile(data.url)
                }
            } else {
                null
            }
        }

    override suspend fun overrideFetch(options: Options, data: PosterData): FetchResult? {
        // Use custom cover if exists
        val useCustomCover = options.parameters.value(USE_CUSTOM_COVER) ?: true
        if (useCustomCover && data.favorite) {
            val customCoverFile = if(data.isMovie){
                movieCoverCache.getCustomCoverFile(data.id)
            } else {
                tvShowCoverCache.getCustomCoverFile(data.id)
            }
            if (customCoverFile.exists()) {
                return SourceResult(
                    source = ImageSource(
                        file = customCoverFile.toOkioPath(),
                        diskCacheKey = keyer.key(data, options).takeIf {
                            !(options.parameters.value(DISABLE_KEYS) ?: false)
                        }
                    ),
                    mimeType = "image/*",
                    dataSource = DataSource.DISK,
                )
            }
        }
        return null
    }

    override suspend fun fetch(options: Options, data: PosterData): Response {
        fun newRequest(): Request {
            val request = Request.Builder()
                .url(data.url!!)
                .headers(options.headers)
                // Support attaching custom data to the network request.
                .tag(Parameters::class.java, options.parameters)

            when {
                options.networkCachePolicy.readEnabled -> {
                    // don't take up okhttp cache
                    request.cacheControl(CACHE_CONTROL_NO_STORE)
                }
                else -> {
                    // This causes the request to fail with a 504 Unsatisfiable Request.
                    request.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
                }
            }

            return request.build()
        }
        return client.newCall(newRequest()).await()
    }

    companion object {
        const val USE_CUSTOM_COVER = "use_custom_cover"
        const val DISABLE_KEYS = "disable_keys"

        private val CACHE_CONTROL_NO_STORE = CacheControl.Builder().noStore().build()
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE = CacheControl.Builder().noCache().onlyIfCached().build()
    }
}
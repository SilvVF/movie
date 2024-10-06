package io.silv.movie.coil.fetchers

import android.content.Context
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import coil.request.Parameters
import coil.size.pxOrElse
import io.silv.core_ui.components.PosterData
import io.silv.movie.coil.core.FetcherDiskStore
import io.silv.movie.coil.core.FetcherDiskStoreImageFile
import io.silv.movie.coil.core.OkHttpFetcherConfig
import io.silv.movie.core.await
import io.silv.movie.data.prefrences.StoragePreferences
import io.silv.movie.presentation.covers.cache.MovieCoverCache
import io.silv.movie.presentation.covers.cache.TVShowCoverCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Path.Companion.toOkioPath

class ContentPosterFetcher(
    override val context: Context,
    private val clientLazy: Lazy<OkHttpClient>,
    private val movieCoverCacheLazy: Lazy<MovieCoverCache>,
    private val tvShowCoverCacheLazy: Lazy<TVShowCoverCache>,
    private val storagePreferences: StoragePreferences,
): OkHttpFetcherConfig<PosterData> {

    private val cacheListItems by lazy {
        runBlocking(Dispatchers.IO) {
            storagePreferences.cacheAllLibraryListPosters.get()
        }
    }

    private val client
        get() = clientLazy.value
    private val movieCoverCache
        get() = movieCoverCacheLazy.value
    private val tvShowCoverCache
        get() = tvShowCoverCacheLazy.value

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
            if (data.favorite || (data.inList && cacheListItems)) {
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
        if (useCustomCover && (data.inList || data.favorite)) {
            val customCoverFile = if(data.isMovie){
                movieCoverCache.getCustomCoverFile(data.id)
            } else {
                tvShowCoverCache.getCustomCoverFile(data.id)
            }
            if (customCoverFile.exists()) {

                return SourceResult(
                    source = ImageSource(
                        file = customCoverFile.toOkioPath(),
                        diskCacheKey = keyer.key(data, options)
                    ),
                    mimeType = "image/*",
                    dataSource = DataSource.DISK,
                )
            }
        }
        return null
    }

    private val sizes = arrayOf(
        "w92" to 92,
        "w154" to 154,
        "w185" to 185,
        "w342" to 342,
        "w500" to 500,
        "w780" to 780,
        "original" to Int.MAX_VALUE // "original" is the largest option
    )

    private fun getClosestSize(widthPx: Int): String {
        // Find the closest size
        val closestSize = sizes.minByOrNull { (_, sizePx) ->
            if (widthPx <= sizePx) sizePx - widthPx else Int.MAX_VALUE
        }

        return closestSize?.first ?: "w185"
    }

    override suspend fun fetch(options: Options, data: PosterData): Response {
        fun newRequest(): Request {
            val request = Request.Builder()
                .url(data.url!!.replace("original", getClosestSize(options.size.width.pxOrElse { -1 })))
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

        private val CACHE_CONTROL_NO_STORE = CacheControl.Builder().noStore().build()
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE = CacheControl.Builder().noCache().onlyIfCached().build()
    }
}
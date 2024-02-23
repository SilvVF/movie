package io.silv.movie

import android.app.Application
import android.content.Context
import android.provider.Settings
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.key.Keyer
import coil.request.Options
import coil.request.Parameters
import coil.util.DebugLogger
import io.silv.core.await
import io.silv.core_ui.components.PosterData
import io.silv.movie.coil.CoilDiskCache
import io.silv.movie.coil.CoilMemoryCache
import io.silv.movie.coil.FetcherDiskStore
import io.silv.movie.coil.FetcherDiskStoreImageFile
import io.silv.movie.coil.OkHttpFetcherConfig
import io.silv.movie.coil.addDiskFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber


class App: Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appModule)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun newImageLoader(): ImageLoader {

        val diskCacheInit = { CoilDiskCache.get(this) }
        val memCacheInit = { CoilMemoryCache.get(this) }
        val client by inject<OkHttpClient>()

        return ImageLoader.Builder(this)
            .diskCache(diskCacheInit)
            .memoryCache(memCacheInit)
            .components {
                addDiskFetcher(
                    diskCache = diskCacheInit,
                    memoryCache = memCacheInit,
                    fetcher = object: OkHttpFetcherConfig<PosterData> {
                        override val keyer: Keyer<PosterData> = Keyer { data, options ->  data.url }
                        override val diskStore: FetcherDiskStore<PosterData> = FetcherDiskStoreImageFile { data, _ -> null }
                        override val context: Context = this@App
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
                                        request.cacheControl(CacheControl.Builder().noStore().build())
                                    }
                                    else -> {
                                        // This causes the request to fail with a 504 Unsatisfiable Request.
                                        request.cacheControl(CacheControl.Builder().noCache().onlyIfCached().build())
                                    }
                                }

                                return request.build()
                            }
                            return client.newCall(newRequest()).await()
                        }
                    }
                )
            }
            .crossfade(
                    300 *
                            Settings.Global.getFloat(
                                this@App.contentResolver,
                                Settings.Global.ANIMATOR_DURATION_SCALE,
                                1f,
                            )
                                .toInt()
                )
            .fetcherDispatcher(Dispatchers.IO.limitedParallelism(8))
            .decoderDispatcher(Dispatchers.IO.limitedParallelism(2))
            .transformationDispatcher(Dispatchers.IO.limitedParallelism(2))
            .apply {
                if (BuildConfig.DEBUG) { logger(DebugLogger()) }
            }
            .build()
    }
}

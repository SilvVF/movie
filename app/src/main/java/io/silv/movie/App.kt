package io.silv.movie

import android.app.Application
import android.provider.Settings
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.DebugLogger
import io.silv.movie.coil.CoilDiskCache
import io.silv.movie.coil.CoilMemoryCache
import io.silv.movie.coil.addDiskFetcher
import io.silv.movie.data.cache.MovieCoverCache
import io.silv.movie.data.cache.TVShowCoverCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
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
            workManagerFactory()
            modules(appModule)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun newImageLoader(): ImageLoader {

        val diskCacheInit = { CoilDiskCache.get(this) }
        val memCacheInit = { CoilMemoryCache.get(this) }
        val client by inject<OkHttpClient>()
        val movieCoverCache by inject<MovieCoverCache>()
        val tvShowCoverCache by inject<TVShowCoverCache>()

        return ImageLoader.Builder(this)
            .diskCache(diskCacheInit)
            .memoryCache(memCacheInit)
            .components {
                addDiskFetcher(
                    diskCache = diskCacheInit,
                    memoryCache = memCacheInit,
                    fetcher = ContentPosterFetcher(
                        this@App,
                        client,
                        movieCoverCache,
                        tvShowCoverCache
                    ),
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

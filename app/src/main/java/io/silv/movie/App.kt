package io.silv.movie

import android.app.Application
import android.os.Build.VERSION.SDK_INT
import android.provider.Settings
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.util.DebugLogger
import io.github.jan.supabase.storage.Storage
import io.silv.movie.coil.CoilDiskCache
import io.silv.movie.coil.CoilMemoryCache
import io.silv.movie.coil.addByteArrayDiskFetcher
import io.silv.movie.coil.addDiskFetcher
import io.silv.movie.data.cache.MovieCoverCache
import io.silv.movie.data.cache.ProfileImageCache
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

        val diskCacheInit = lazy(LazyThreadSafetyMode.SYNCHRONIZED) { CoilDiskCache.get(this) }
        val memCacheInit= lazy(LazyThreadSafetyMode.SYNCHRONIZED) { CoilMemoryCache.get(this) }

        return ImageLoader.Builder(this)
            .diskCache(diskCacheInit.value)
            .memoryCache(memCacheInit.value)
            .components {
                addDiskFetcher(
                    diskCache = diskCacheInit,
                    memoryCache = memCacheInit,
                    fetcher = ContentPosterFetcher(
                        this@App,
                        inject<OkHttpClient>(),
                        inject<MovieCoverCache>(),
                        inject<TVShowCoverCache>()
                    ),
                )
                addByteArrayDiskFetcher(
                    diskCache = diskCacheInit,
                    memoryCache = memCacheInit,
                    fetcher = UserProfileImageFetcher(
                        this@App,
                            inject<Storage>(),
                            inject<ProfileImageCache>()
                    )
                )
                addByteArrayDiskFetcher(
                    diskCache = diskCacheInit,
                    memoryCache = memCacheInit,
                    fetcher = BucketItemFetcher(
                        this@App,
                        inject<Storage>(),
                        inject<ProfileImageCache>()
                    )
                )
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
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

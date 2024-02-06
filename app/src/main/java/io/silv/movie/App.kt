package io.silv.movie

import android.app.Application
import android.content.Context
import android.provider.Settings
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.key.Keyer
import coil.request.Options
import coil.util.DebugLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.silv.coil_disk_fetcher.DiskBackedFetcher
import io.silv.coil_disk_fetcher.FetcherDiskStore
import io.silv.coil_disk_fetcher.addDiskFetcher
import io.silv.movie.ui.components.Poster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.io.File

class App: Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appModule)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun newImageLoader(): ImageLoader {
        val client by inject<HttpClient>()
        return ImageLoader.Builder(this)
            .addDiskFetcher(
                    object: DiskBackedFetcher<Poster> {
                        override val keyer: Keyer<Poster> = Keyer { data, _ -> "${data.id}" }
                        override val diskStore: FetcherDiskStore<Poster> =
                            object : FetcherDiskStore<Poster> {
                                override fun getImageFile(data: Poster, options: Options): File? = null
                            }
                        override val context: Context
                            get() = this@App
                        override suspend fun fetch(options: Options, data: Poster): ByteArray {
                            val httpResponse: HttpResponse = client.get(data.url)
                            return httpResponse.body<ByteArray>()
                        }
                    }
                )
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
            .logger(DebugLogger())
            .build()
    }
}

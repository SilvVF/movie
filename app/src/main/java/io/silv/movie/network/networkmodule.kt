package io.silv.movie.network

import com.google.net.cronet.okhttptransport.CronetCallFactory
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.silv.movie.core.MB
import io.silv.movie.core.toBytes
import io.silv.movie.network.ratelimit.rateLimit
import io.silv.movie.network.service.piped.PipedApi
import io.silv.movie.network.service.tmdb.TMDBAuthInterceptor
import io.silv.movie.network.service.tmdb.TMDBMovieService
import io.silv.movie.network.service.tmdb.TMDBTVShowService
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.chromium.net.CronetEngine
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.create
import java.io.File
import kotlin.time.Duration.Companion.seconds

typealias TMDBClient = OkHttpClient

val networkModule =
    module {

        single {
            Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            }
        }

        single<TMDBClient> {
            OkHttpClient()
                .newBuilder()
                .cache(
                    Cache(
                        directory = File(androidContext().cacheDir, "network_cache"),
                        maxSize = 5L.MB.toBytes()
                    )
                )
                .addInterceptor(TMDBAuthInterceptor())
                .rateLimit(
                    permits = 50,
                    period = 1.seconds
                )
                .build()
        }

        single {
            val engine = CronetEngine.Builder(androidContext())
                .enableHttp2(true)
                .enableQuic(true)
                .enableBrotli(true)
                .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_IN_MEMORY, 1024L * 1024L) // 1MiB
                .build()
            val callFactory = CronetCallFactory.newBuilder(engine).build()

            Retrofit.Builder()
                .baseUrl("https://pipedapi.kavin.rocks")
                .callFactory(callFactory)
                .addConverterFactory(
                    get<Json>().asConverterFactory("application/json".toMediaType())
                )
                .build()
                .create<PipedApi>()
        }

        single {
            Retrofit.Builder()
                .baseUrl("https://api.themoviedb.org/3/")
                .addConverterFactory(
                    get<Json>().asConverterFactory("application/json".toMediaType())
                )
                .client(get<TMDBClient>())
                .build()
        }

        single {
            get<Retrofit>().create<TMDBTVShowService>()
        }

        single {
            get<Retrofit>().create<TMDBMovieService>()
        }
    }
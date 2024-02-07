package io.silv.core_network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.silv.core.MB
import io.silv.core.toBytes
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.create
import java.io.File

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
                .build()
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
            get<Retrofit>().create<TMDBMovieService>()
        }
    }
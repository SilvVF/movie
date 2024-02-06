package io.silv.movie

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.silv.movie.data.AuthInterceptor
import io.silv.movie.data.MovieApi
import io.silv.movie.data.SessionInterceptor
import io.silv.movie.domain.movie.GetPagingSource
import io.silv.movie.presentation.movie.MovieViewModel
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.io.File

val appModule =
    module {

        viewModelOf(::MovieViewModel)

        factoryOf(::GetPagingSource)

        single {
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            }
        }


        single {
            HttpClient(engine = OkHttp) {
                engine {
                    this.
                    OkHttpClient()
                        .newBuilder()
                        .addInterceptor(AuthInterceptor())
                        .addInterceptor(
                            SessionInterceptor(json = get<Json>())
                        )
                        .cache(
                            Cache(
                                File(androidContext().cacheDir, "network_cache"),
                                5_000L
                            )
                        )
                        .build()
                }
                install(ContentNegotiation) {
                    json(get<Json>())
                }
            }
        }

        singleOf(::MovieApi)
    }

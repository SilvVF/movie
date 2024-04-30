package io.silv.movie.network

import com.google.net.cronet.okhttptransport.CronetCallFactory
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.silv.movie.BuildConfig
import io.silv.movie.core.MB
import io.silv.movie.core.toBytes
import io.silv.movie.network.ratelimit.rateLimit
import io.silv.movie.network.service.piped.PipedApi
import io.silv.movie.network.service.tmdb.TMDBAuthInterceptor
import io.silv.movie.network.service.tmdb.TMDBMovieService
import io.silv.movie.network.service.tmdb.TMDBPersonService
import io.silv.movie.network.service.tmdb.TMDBRecommendationService
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

        single {
            Cache(
                directory = File(androidContext().cacheDir, "network_cache"),
                maxSize = 5L.MB.toBytes()
            )
        }

        single {
            HttpClient(OkHttp) {
                engine {
                    config {
                        cache(get())
                    }
                }
            }
        }

        single<SupabaseClient> {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey =  BuildConfig.SUPABSE_ANON_KEY,
            ) {
                install(Postgrest)
                install(Auth)
                install(ComposeAuth) {
                    googleNativeLogin(serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)
                }
                install(Storage)
                install(Realtime)
            }
        }

        single<Storage>{ get<SupabaseClient>().storage }
        single<Auth> { get<SupabaseClient>().auth }
        single<Postgrest> { get<SupabaseClient>().postgrest }
        single<ComposeAuth> { get<SupabaseClient>().composeAuth }

        single<TMDBClient> {
            OkHttpClient()
                .newBuilder()
                .cache(get())
                .addInterceptor(TMDBAuthInterceptor())
                .rateLimit(
                    permits = 50,
                    period = 1.seconds
                )
                .build()
        }

        single {
           CronetEngine.Builder(androidContext())
                .enableHttp2(true)
                .enableQuic(true)
                .enableBrotli(true)
                .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_IN_MEMORY, 1024L * 1024L) // 1MiB
                .build()
        }

        single {
            Retrofit.Builder()
                .baseUrl("https://pipedapi.adminforge.de/")
                .callFactory(
                    CronetCallFactory.newBuilder(get()).build()
                )
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

        single { get<Retrofit>().create<TMDBRecommendationService>() }

        single { get<Retrofit>().create<TMDBPersonService>() }

        single {
            get<Retrofit>().create<TMDBTVShowService>()
        }

        single {
            get<Retrofit>().create<TMDBMovieService>()
        }
    }
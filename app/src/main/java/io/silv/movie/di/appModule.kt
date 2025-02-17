package io.silv.movie.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.silv.Database
import io.silv.Movie
import io.silv.Show
import io.silv.movie.BuildConfig
import io.silv.movie.core.NetworkMonitor
import io.silv.movie.data.local.CreditRepository
import io.silv.movie.data.local.CreditRepositoryImpl
import io.silv.movie.data.local.LocalContentDelegate
import io.silv.movie.data.local.LocalContentRepositoryImpl
import io.silv.movie.data.local.MovieRepository
import io.silv.movie.data.local.MovieRepositoryImpl
import io.silv.movie.data.local.ShowRepository
import io.silv.movie.data.local.ShowRepositoryImpl
import io.silv.movie.data.local.TrailerRepository
import io.silv.movie.data.local.TrailerRepositoryImpl
import io.silv.movie.data.network.NetworkContentDelegate
import io.silv.movie.data.network.NetworkContentDelegateImpl
import io.silv.movie.data.network.SourceCreditsRepository
import io.silv.movie.data.network.SourceMovieRepository
import io.silv.movie.data.network.SourceMovieRepositoryImpl
import io.silv.movie.data.network.SourceShowRepository
import io.silv.movie.data.network.SourceShowRepositoryImpl
import io.silv.movie.data.network.SourceTrailerRepository
import io.silv.movie.prefrences.BasePreferences
import io.silv.movie.prefrences.BrowsePreferences
import io.silv.movie.prefrences.LibraryPreferences
import io.silv.movie.prefrences.StoragePreferences
import io.silv.movie.prefrences.UiPreferences
import io.silv.movie.prefrences.core.DatastorePreferenceStore
import io.silv.movie.prefrences.core.PreferenceStore
import io.silv.movie.data.ListUpdateManager
import io.silv.movie.data.supabase.CommentsRepository
import io.silv.movie.data.supabase.ListRepository
import io.silv.movie.data.supabase.BackendRepository
import io.silv.movie.data.supabase.BackendRepositoryImpl
import io.silv.movie.data.workers.FavoritesUpdateWorker
import io.silv.movie.data.workers.ListUpdateWorker
import io.silv.movie.data.workers.RecommendationWorker
import io.silv.movie.data.workers.UserListUpdateWorker
import io.silv.movie.data.local.ContentListRepository
import io.silv.movie.data.local.ContentListRepositoryImpl
import io.silv.movie.data.RecommendationManager
import io.silv.movie.data.DeleteContentList
import io.silv.movie.data.EditContentList
import io.silv.movie.database.DBAdapters.listIntAdapter
import io.silv.movie.database.DBAdapters.listStringAdapter
import io.silv.movie.database.DatabaseHandlerImpl
import io.silv.movie.network.service.tmdb.TMDBMovieService
import io.silv.movie.network.service.tmdb.TMDBPersonService
import io.silv.movie.network.service.tmdb.TMDBRecommendationService
import io.silv.movie.network.service.tmdb.TMDBTVShowService
import io.silv.movie.presentation.covers.ImageSaver
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.create
import io.silv.movie.presentation.covers.cache.ListCoverCache
import io.silv.movie.presentation.covers.cache.MovieCoverCache
import io.silv.movie.presentation.covers.cache.ProfileImageCache
import io.silv.movie.presentation.covers.cache.TVShowCoverCache
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import io.silv.core_ui.voyager.ScreenResultsViewModel
import io.silv.movie.DefaultDispatcher
import io.silv.movie.IoDispatcher
import io.silv.movie.MainDispatcher
import io.silv.movie.MainViewModel
import io.silv.movie.core.MB
import io.silv.movie.core.toBytes
import io.silv.movie.data.supabase.UserRepository
import io.silv.movie.database.DatabaseHandler
import io.silv.movie.network.ratelimit.rateLimit
import io.silv.movie.network.service.piped.PipedApi
import io.silv.movie.network.service.tmdb.TMDBAuthInterceptor
import io.silv.movie.presentation.components.profile.ProfileScreenModel
import io.silv.movie.presentation.covers.screenmodel.ListCoverScreenModel
import io.silv.movie.presentation.covers.screenmodel.MovieCoverScreenModel
import io.silv.movie.presentation.covers.screenmodel.TVCoverScreenModel
import io.silv.movie.presentation.media.PlayerViewModel
import io.silv.movie.presentation.screen.ProfileViewScreenModel
import io.silv.movie.presentation.screenmodel.AddToListScreenModel
import io.silv.movie.presentation.screenmodel.BrowseListsScreenModel
import io.silv.movie.presentation.screenmodel.CommentsScreenModel
import io.silv.movie.presentation.screenmodel.CreditsViewScreenModel
import io.silv.movie.presentation.screenmodel.FavoritesScreenModel
import io.silv.movie.presentation.screenmodel.LibraryScreenModel
import io.silv.movie.presentation.screenmodel.ListAddScreenModel
import io.silv.movie.presentation.screenmodel.ListPagedScreenModel
import io.silv.movie.presentation.screenmodel.ListViewScreenModel
import io.silv.movie.presentation.screenmodel.MovieScreenModel
import io.silv.movie.presentation.screenmodel.MovieViewScreenModel
import io.silv.movie.presentation.screenmodel.PersonViewScreenModel
import io.silv.movie.presentation.screenmodel.SearchForListScreenModel
import io.silv.movie.presentation.screenmodel.SelectProfileImageScreenModel
import io.silv.movie.presentation.screenmodel.TVScreenModel
import io.silv.movie.presentation.screenmodel.TVViewScreenModel
import kotlinx.coroutines.Dispatchers
import okhttp3.Cache
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.qualifier
import java.io.File
import kotlin.time.Duration.Companion.seconds

typealias TMDBClient = OkHttpClient

private val android.content.Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val appModule = module {
    single<SqlDriver> {
        AndroidSqliteDriver(
            Database.Schema,
            androidContext(),
            "movie.db",
            callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
    }


    single {
        Database(
            driver = get<SqlDriver>(),
            movieAdapter = Movie.Adapter(
                genreIdsAdapter = listIntAdapter,
                genresAdapter = listStringAdapter,
                production_companiesAdapter = listStringAdapter
            ),
            showAdapter = Show.Adapter(
                genre_idsAdapter = listIntAdapter,
                genresAdapter = listStringAdapter,
                production_companiesAdapter = listStringAdapter
            )
        )
    }

    single<DatabaseHandler> { DatabaseHandlerImpl(get(), get(), get()) }

    single {
        Dispatchers.Default
    } withOptions {
        qualifier<DefaultDispatcher>()
    }

    single {
        Dispatchers.IO
    } withOptions {
        qualifier<IoDispatcher>()
    }

    single {
        Dispatchers.Main
    } withOptions {
        qualifier<MainDispatcher>()
    }

    factoryOf(::SourceTrailerRepository)

    singleOf(::CreditRepositoryImpl) { bind<CreditRepository>() }

    singleOf(::ContentListRepositoryImpl) { bind<ContentListRepository>() }

    singleOf(::TrailerRepositoryImpl) { bind<TrailerRepository>() }

    singleOf(::SourceMovieRepositoryImpl) { bind<SourceMovieRepository>() }

    singleOf(::SourceShowRepositoryImpl) { bind<SourceShowRepository>() }

    singleOf(::MovieRepositoryImpl) { bind<MovieRepository>() }

    singleOf(::ShowRepositoryImpl) { bind<ShowRepository>() }

    factoryOf(::LocalContentRepositoryImpl) { bind<LocalContentDelegate>() }

    factoryOf(::NetworkContentDelegateImpl) { bind<NetworkContentDelegate>() }

    factoryOf(::SourceCreditsRepository)

    singleOf(::BrowsePreferences)

    singleOf(::LibraryPreferences)

    singleOf(::BasePreferences)

    single<PreferenceStore> {
        DatastorePreferenceStore(androidContext().dataStore)
    }

    singleOf(::BackendRepositoryImpl) { bind<BackendRepository>() }
    single<ListRepository> { get<BackendRepository>() }
    single<UserRepository> { get<BackendRepository>() }

    singleOf(::CommentsRepository)

    singleOf(::UiPreferences)

    workerOf(::RecommendationWorker)

    workerOf(::FavoritesUpdateWorker)

    workerOf(::UserListUpdateWorker)

    workerOf(::ListUpdateWorker)

    singleOf(::ListUpdateManager)

    factoryOf(::EditContentList)

    factoryOf(::DeleteContentList)

    singleOf(::RecommendationManager)

    singleOf(::StoragePreferences)

    singleOf(::MovieCoverCache)

    singleOf(::ProfileImageCache)

    singleOf(::TVShowCoverCache)

    singleOf(::ListCoverCache)

    singleOf(::ImageSaver)

    singleOf(::NetworkMonitor)

    single<Json> {
        Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }
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
    single { get<Retrofit>().create<TMDBTVShowService>() }
    single { get<Retrofit>().create<TMDBMovieService>() }

    single<SupabaseClient> {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABSE_ANON_KEY,
        ) {
            install(Postgrest)
            install(Auth)
            install(ComposeAuth) {
                googleNativeLogin(serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)
            }
            install(Storage)
        }
    }

    single { get<SupabaseClient>().storage }
    single { get<SupabaseClient>().auth }
    single { get<SupabaseClient>().postgrest }
    single { get<SupabaseClient>().composeAuth }

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
        PipedApi(
            OkHttpClient.Builder()
                .cache(get())
                .build(),
            get()
        )
    }
}

val screenModelModule = module {

    viewModelOf(::MainViewModel)

    viewModelOf(::ScreenResultsViewModel)

    factoryOf(::MovieScreenModel)

    viewModelOf(::PlayerViewModel)

    factoryOf(::MovieViewScreenModel)

    factoryOf(::TVScreenModel)

    factoryOf(::FavoritesScreenModel)

    factoryOf(::TVViewScreenModel)

    factoryOf(::LibraryScreenModel)

    factoryOf(::ListViewScreenModel)

    factoryOf(::ListAddScreenModel)

    factoryOf(::MovieCoverScreenModel)

    factoryOf(::CreditsViewScreenModel)

    factoryOf(::CommentsScreenModel)

    factoryOf(::ProfileScreenModel)

    factoryOf(::TVCoverScreenModel)

    factoryOf(::ListCoverScreenModel)

    factoryOf(::BrowseListsScreenModel)

    factoryOf(::PersonViewScreenModel)

    factoryOf(::AddToListScreenModel)

    factoryOf(::SearchForListScreenModel)

    factoryOf(::ListPagedScreenModel)

    factoryOf(::SelectProfileImageScreenModel)

    factoryOf(::ProfileViewScreenModel)
}
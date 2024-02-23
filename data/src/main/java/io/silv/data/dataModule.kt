package io.silv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.silv.core_database.databaseModule
import io.silv.core_network.networkModule
import io.silv.data.movie.interactor.GetMovie
import io.silv.data.movie.interactor.GetRemoteMovie
import io.silv.data.movie.interactor.GetRemoteTVShows
import io.silv.data.movie.interactor.NetworkToLocalMovie
import io.silv.data.movie.interactor.UpdateMovie
import io.silv.data.movie.repository.MovieRepository
import io.silv.data.movie.repository.MovieRepositoryImpl
import io.silv.data.movie.repository.SourceMovieRepository
import io.silv.data.movie.repository.SourceMovieRepositoryImpl
import io.silv.data.movie.repository.SourceTVRepository
import io.silv.data.movie.repository.SourceTVRepositoryImpl
import io.silv.data.prefrences.TMDBPreferences
import io.silv.data.prefrences.core.DatastorePreferenceStore
import io.silv.data.prefrences.core.PreferenceStore
import io.silv.data.trailers.GetMovieTrailers
import io.silv.data.trailers.GetRemoteTrailers
import io.silv.data.trailers.NetworkToLocalTrailer
import io.silv.data.trailers.TrailerRepository
import io.silv.data.trailers.TrailerRepositoryImpl
import io.silv.data.tv.GetShow
import io.silv.data.tv.NetworkToLocalTVShow
import io.silv.data.tv.ShowRepository
import io.silv.data.tv.ShowRepositoryImpl
import io.silv.data.tv.UpdateShow
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule =
    module {

        includes(databaseModule)
        includes(networkModule)

        factoryOf(::GetMovie)

        factoryOf(::UpdateShow)

        factoryOf(::GetShow)

        factoryOf(::UpdateMovie)

        factoryOf(::NetworkToLocalTVShow)

        factoryOf(::GetRemoteMovie)

        factoryOf(::GetRemoteTVShows)

        factoryOf(::GetMovieTrailers)

        factoryOf(::NetworkToLocalMovie)

        factoryOf(::NetworkToLocalTrailer)

        factoryOf(::GetRemoteTrailers)

        singleOf(::TrailerRepositoryImpl) { bind<TrailerRepository>() }

        singleOf(::SourceMovieRepositoryImpl) { bind<SourceMovieRepository>() }

        singleOf(::SourceTVRepositoryImpl) { bind<SourceTVRepository>() }

        singleOf(::MovieRepositoryImpl) { bind<MovieRepository>() }

        singleOf(::ShowRepositoryImpl) { bind<ShowRepository>() }

        singleOf(::TMDBPreferences)

        single<PreferenceStore> {
            DatastorePreferenceStore(androidContext().dataStore)
        }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
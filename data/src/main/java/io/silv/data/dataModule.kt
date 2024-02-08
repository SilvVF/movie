package io.silv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.silv.core_database.databaseModule
import io.silv.core_network.networkModule
import io.silv.data.movie.interactor.GetMovie
import io.silv.data.movie.interactor.GetRemoteMovie
import io.silv.data.movie.interactor.NetworkToLocalMovie
import io.silv.data.movie.repository.MovieRepository
import io.silv.data.movie.repository.MovieRepositoryImpl
import io.silv.data.movie.repository.SourceMovieRepository
import io.silv.data.movie.repository.SourceMovieRepositoryImpl
import io.silv.data.prefrences.TMDBPreferences
import io.silv.data.prefrences.core.DatastorePreferenceStore
import io.silv.data.prefrences.core.PreferenceStore
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

        factoryOf(::GetRemoteMovie)

        factoryOf(::NetworkToLocalMovie)

        singleOf(::SourceMovieRepositoryImpl) { bind<SourceMovieRepository>() }

        singleOf(::MovieRepositoryImpl) { bind<MovieRepository>() }

        singleOf(::TMDBPreferences)

        single<PreferenceStore> {
            DatastorePreferenceStore(androidContext().dataStore)
        }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
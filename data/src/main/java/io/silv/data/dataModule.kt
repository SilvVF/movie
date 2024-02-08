package io.silv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.silv.data.prefrences.DatastorePreferenceStore
import io.silv.data.prefrences.PreferenceStore
import io.silv.data.prefrences.TMDBPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule =
    module {

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
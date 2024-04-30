package io.silv.movie.data.prefrences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.silv.movie.data.prefrences.core.DatastorePreferenceStore
import io.silv.movie.data.prefrences.core.PreferenceStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val preferenceModule = module {

    singleOf(::BrowsePreferences)

    singleOf(::LibraryPreferences)

    singleOf(::BasePreferences)

    single<PreferenceStore> {
        DatastorePreferenceStore(androidContext().dataStore)
    }
}
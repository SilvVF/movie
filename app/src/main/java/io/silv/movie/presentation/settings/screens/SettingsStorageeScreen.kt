package io.silv.movie.presentation.settings.screens

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.silv.movie.R
import io.silv.movie.data.prefrences.StoragePreferences
import io.silv.movie.presentation.settings.Preference
import io.silv.movie.presentation.settings.SearchableSettings
import org.koin.compose.koinInject


data object SettingsStorageeScreen: SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = R.string.storage

    @Composable
    override fun getPreferences(): List<Preference> {
        val storagePreferences = koinInject<StoragePreferences>()

        return listOf(
            getCacheGroup(prefs = storagePreferences),
        )
    }

    @Composable
    private fun getCacheGroup(
        prefs: StoragePreferences,
    ): Preference.PreferenceGroup {

        val context = LocalContext.current

        val diskCacheMaxSizeMB = remember(prefs) { prefs.cacheMaxSizeMB }
        val diskCachePct = remember(prefs) { prefs.cacheSizePct }
        val cacheAllPosters = remember(prefs) { prefs.cacheAllLibraryListPosters }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.cache),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = cacheAllPosters,
                    title = stringResource(id = R.string.cache_list_posters),
                    subtitle =  stringResource(id = R.string.cache_all_library_posters),
                    onValueChanged = {
                        cacheAllPosters.set(it)
                        it
                    }
                ),
                Preference.PreferenceItem.FloatSliderPreference(
                    pref = diskCachePct,
                    min = 0.05f,
                    max = 1f,
                    steps = 500,
                    title = stringResource(id = R.string.disk_cache_pct),
                    subtitleProvider = {
                        stringResource(id = R.string.disk_cache_pct_sub, it)
                    },
                    onValueChanged = {
                        diskCachePct.set(it)
                        Toast.makeText(context, R.string.requires_app_restart, Toast.LENGTH_SHORT).show()
                        true
                    },
                    subtitle = stringResource(id = R.string.disk_cache_pct_sub),
                ),
                Preference.PreferenceItem.SliderPreference(
                    pref = diskCacheMaxSizeMB,
                    min = 16,
                    max = 512,
                    title = stringResource(id = R.string.disk_cache_max_size_mb),
                    onValueChanged = {
                        diskCacheMaxSizeMB.set(it)
                        Toast.makeText(context, R.string.requires_app_restart, Toast.LENGTH_SHORT).show()
                        true
                    },
                    subtitleProvider = {
                        stringResource(id = R.string.disk_cache_max_size_mb_sub, it)
                    },
                    subtitle = stringResource(id = R.string.disk_cache_max_size_mb),
                )
            ),
        )
    }
}


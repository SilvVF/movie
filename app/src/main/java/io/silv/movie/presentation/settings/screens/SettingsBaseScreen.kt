package io.silv.movie.presentation.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import io.silv.movie.R
import io.silv.movie.api.service.piped.PipedApi
import io.silv.movie.prefrences.BasePreferences
import io.silv.movie.presentation.settings.Preference
import io.silv.movie.presentation.settings.SearchableSettings
import org.koin.compose.koinInject

object SettingsBaseScreen: SearchableSettings {

    private fun readResolve(): Any = SettingsBaseScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = R.string.pref_category_base

    @Composable
    override fun getPreferences(): List<Preference> {
        val basePreferences = koinInject<BasePreferences>()
        val pipedApi = koinInject<PipedApi>()

        return listOf(
            getBaseGroup(basePreferences, pipedApi)
        )
    }
    @Composable
    private fun getBaseGroup(
        basePreferences: BasePreferences,
        pipedApi: PipedApi
    ): Preference.PreferenceGroup {

        val pipedUrl = remember(basePreferences) { basePreferences.pipedUrl() }
        val providers by produceState(emptyList()) {
            value = pipedApi.getUrlList().getOrDefault(emptyList())
        }

        val providerEntries by remember(providers) {
            derivedStateOf {
                buildMap {
                    providers.forEach {
                        put(it.url.trim(), "${it.name} | ${it.cdn} | ${it.languages.joinToString()}")
                    }
                }
            }
        }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.pref_category_base),
            enabled = true,
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference<String>(
                    pref = pipedUrl,
                    title = "Piped Url",
                    entries = providerEntries,
                    onValueChanged = { url ->
                        pipedUrl.set(url)
                        true
                    }
                )
            )
        )
    }
}
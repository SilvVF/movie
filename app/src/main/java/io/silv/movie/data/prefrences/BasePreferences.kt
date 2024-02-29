package io.silv.movie.data.prefrences

import io.silv.movie.data.prefrences.core.PreferenceStore

class BasePreferences(
    private val settings: PreferenceStore,
) {

    fun incognitoMode() = settings.getBoolean("incognito_mode", false)

    fun autoplay() = settings.getBoolean("autoplay", true)
}

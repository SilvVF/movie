package io.silv.data.prefrences

import io.silv.data.prefrences.core.PreferenceStore

class BasePreferences(
    private val settings: PreferenceStore,
) {

    fun incognitoMode() = settings.getBoolean("incognito_mode", false)
}

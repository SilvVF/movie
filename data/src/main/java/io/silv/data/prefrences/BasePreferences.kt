package io.silv.data.prefrences

class BasePreferences(
    private val settings: PreferenceStore,
) {

    fun incognitoMode() = settings.getBoolean("incognito_mode", false)
}

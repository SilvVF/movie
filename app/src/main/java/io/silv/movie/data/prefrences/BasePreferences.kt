package io.silv.movie.data.prefrences

import io.silv.movie.data.prefrences.core.PreferenceStore
import io.silv.movie.data.user.User

class BasePreferences(
    private val settings: PreferenceStore,
) {

    fun incognitoMode() = settings.getBoolean("incognito_mode", false)

    fun autoplay() = settings.getBoolean("autoplay", true)

    fun recentlyViewedLists() = settings.getStringSet("recent_lists")

    fun savedUser() = settings.getObject(
        key = "saved_user",
        defaultValue = null,
        serializer = { it?.serialize() ?: "" },
        deserializer = User::deserialize
    )
}


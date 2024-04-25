package io.silv.movie.data.prefrences

import io.silv.movie.data.prefrences.core.PreferenceStore
import io.silv.movie.data.user.User
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BasePreferences(
    private val settings: PreferenceStore,
) {

    fun incognitoMode() = settings.getBoolean("incognito_mode", false)

    fun autoplay() = settings.getBoolean("autoplay", true)

    fun recentlyViewedLists() = settings.getObject<List<Pair<Long, String>>>(
        "sorted_recent_lists",
        serializer = { Json.encodeToString(it) },
        deserializer = Json::decodeFromString,
        defaultValue = emptyList()
    )

    fun savedUser() = settings.getObject(
        key = "saved_user",
        defaultValue = null,
        serializer = { it?.serialize() ?: "" },
        deserializer = User::deserialize
    )
}


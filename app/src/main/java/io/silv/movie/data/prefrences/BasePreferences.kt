package io.silv.movie.data.prefrences

import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.data.prefrences.core.PreferenceStore
import io.silv.movie.data.prefrences.core.getAndSet
import io.silv.movie.data.user.User
import kotlinx.datetime.Clock
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

    fun recentEmojis() = settings.getStringSet(
        "recent_emojis",
        defaultValue = setOf(
            "\uD83E\uDD23",
            "\uD83D\uDE43",
            "\uD83D\uDE07",
            "\uD83D\uDE00",
            "\uD83E\uDD72",
            "\uD83E\uDD13",
            "\uD83D\uDE28",
            "\uD83D\uDE21"
        )
    )

    companion object {
       suspend fun BasePreferences.addToRecentlyViewed(list: ContentList) {
           recentlyViewedLists().getAndSet { recent ->
               val lst = recent.toMutableList()
               lst.apply {
                   val idx = indexOfFirst { it.second == list.supabaseId }
                   if (idx != -1) {
                       lst.removeAt(idx)
                   }
                   sortBy { it.first }
                   if (list.supabaseId != null) {
                       add(Clock.System.now().epochSeconds to list.supabaseId)
                   }
                   if (size > 20) {
                       removeAt(0)
                   }
               }
           }
       }
    }
}


package io.silv.movie.data.prefrences

import io.silv.movie.data.prefrences.core.PreferenceStore
import io.silv.movie.presentation.library.browse.LibrarySortMode

class LibraryPreferences(
    private val preferenceStore: PreferenceStore
) {

    fun displayInList() = preferenceStore.getBoolean(
        "pref_display_mode_library",
        true
    )

    fun listViewDisplayMode() = preferenceStore.getObject(
        "pref_display_mode_list_view",
        PosterDisplayMode.List,
        PosterDisplayMode.Serializer::serialize,
        PosterDisplayMode.Serializer::deserialize,
    )

    fun sortMode() = preferenceStore.getObject(
        "pref_sort_mode_library",
        LibrarySortMode.Title,
        serializer = { mode: LibrarySortMode ->
            when(mode) {
                LibrarySortMode.Count -> "C"
                LibrarySortMode.RecentlyAdded -> "R"
                LibrarySortMode.Title -> "T"
            }
        },
        deserializer = {
            when (it) {
                "C" -> LibrarySortMode.Count
                "R" -> LibrarySortMode.RecentlyAdded
                else -> LibrarySortMode.Title
            }
        }
    )
}

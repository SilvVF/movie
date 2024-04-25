package io.silv.movie.data.prefrences

import io.silv.movie.data.prefrences.core.PreferenceStore

class BrowsePreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun browsePosterDisplayMode() = preferenceStore.getObject(
        "pref_browse_poster_display_mode",
        PosterDisplayMode.default,
        PosterDisplayMode.Serializer::serialize,
        PosterDisplayMode.Serializer::deserialize,
    )

    fun browseGridCellCount() = preferenceStore.getInt("pref_browse_grid_cell_count", 2)


    fun browseHideLibraryItems() = preferenceStore.getBoolean("pref_browse_hide_library_items", false)

    fun showAdultSource() = preferenceStore.getBoolean("show_adult_source", false)
}
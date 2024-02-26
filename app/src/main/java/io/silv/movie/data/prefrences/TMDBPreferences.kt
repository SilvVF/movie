package io.silv.movie.data.prefrences

import io.silv.movie.data.prefrences.core.PreferenceStore

class TMDBPreferences(
    private val preferenceStore: PreferenceStore,
) {

    // Common options

    fun sourceDisplayMode() = preferenceStore.getObject(
        "pref_display_mode_catalogue",
        PosterDisplayMode.default,
        PosterDisplayMode.Serializer::serialize,
        PosterDisplayMode.Serializer::deserialize,
    )

    fun gridCellsCount() = preferenceStore.getInt("pref_grid_cells_count", 2)



    fun hideLibraryItems() = preferenceStore.getBoolean("hide_library_items", false)

    fun showAdultSource() = preferenceStore.getBoolean("show_adult_source", false)
}
package io.silv.data.prefrences

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


    fun hideLibraryItems() = preferenceStore.getBoolean("hide_library_items", false)

    fun showAdultSource() = preferenceStore.getBoolean("show_adult_source", false)
}
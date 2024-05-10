package io.silv.movie.data.prefrences

import io.silv.movie.data.prefrences.core.PreferenceStore

class StoragePreferences(
    settings: PreferenceStore,
) {

    val cacheMaxSizeMB = settings.getInt("cache_max_size", 150)

    val cacheSizePct = settings.getFloat("cache_max_pct", 0.2f)

    val cacheAllLibraryListPosters = settings.getBoolean("cache_all_library_list_posters", true)
}
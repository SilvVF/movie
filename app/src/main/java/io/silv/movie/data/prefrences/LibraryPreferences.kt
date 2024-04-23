package io.silv.movie.data.prefrences

import io.silv.movie.data.prefrences.core.PreferenceStore
import io.silv.movie.presentation.library.screenmodels.FavoritesSortMode
import io.silv.movie.presentation.library.screenmodels.LibrarySortMode
import io.silv.movie.presentation.library.screenmodels.ListSortMode

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

    fun sortModeFavorites() = preferenceStore.getObject(
        "pref_sort_mode_library_favorites",
        FavoritesSortMode.Title,
        serializer = { mode: FavoritesSortMode ->
            when(mode) {
                FavoritesSortMode.Title -> "T"
                FavoritesSortMode.Show -> "S"
                FavoritesSortMode.Movie -> "M"
                FavoritesSortMode.RecentlyAdded -> "R"
            }
        },
        deserializer = {
            when (it) {
                "S" -> FavoritesSortMode.Show
                "M" -> FavoritesSortMode.Movie
                "R" -> FavoritesSortMode.RecentlyAdded
                else -> FavoritesSortMode.Title
            }
        }
    )

    fun sortModeList() = preferenceStore.getObject(
        "pref_sort_mode_list",
        ListSortMode.RecentlyAdded(false),
        serializer = { mode: ListSortMode ->
            "${if (mode.ascending) '1' else '0'}" + when(mode) {
                is ListSortMode.Title -> "T"
                is ListSortMode.Show -> "S"
                is ListSortMode.Movie -> "M"
                is ListSortMode.RecentlyAdded -> "R"
            }
        },
        deserializer = {
            val asc = it.first() == '1'
            when (it.last()) {
                'S' -> ListSortMode.Show(asc)
                'M' -> ListSortMode.Movie(asc)
                'R' -> ListSortMode.RecentlyAdded(asc)
                else -> ListSortMode.Title(asc)
            }
        }
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

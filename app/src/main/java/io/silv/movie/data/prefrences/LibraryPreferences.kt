package io.silv.movie.data.prefrences

import io.silv.movie.data.prefrences.core.PreferenceStore
import io.silv.movie.presentation.library.browse.LibrarySortMode
import io.silv.movie.presentation.library.view.favorite.FavoritesSortMode
import io.silv.movie.presentation.library.view.list.ListSortMode

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
        "pref_sort_mode_library_list",
        ListSortMode.Title,
        serializer = { mode: ListSortMode ->
            when(mode) {
                ListSortMode.Title -> "T"
                ListSortMode.Show -> "S"
                ListSortMode.Movie -> "M"
                ListSortMode.RecentlyAdded -> "R"
            }
        },
        deserializer = {
            when (it) {
                "S" -> ListSortMode.Show
                "M" -> ListSortMode.Movie
                "R" -> ListSortMode.RecentlyAdded
                else -> ListSortMode.Title
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

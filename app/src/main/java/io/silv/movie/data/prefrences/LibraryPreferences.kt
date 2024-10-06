package io.silv.movie.data.prefrences

import io.silv.movie.data.prefrences.core.PreferenceStore
import io.silv.movie.presentation.screenmodel.FavoritesSortMode
import io.silv.movie.presentation.screenmodel.LibrarySortMode
import io.silv.movie.presentation.screenmodel.ListSortMode

class LibraryPreferences(
    private val preferenceStore: PreferenceStore
) {
    fun libraryDisplayInList() = preferenceStore.getBoolean(
        "pref_library_display_in_list",
        true
    )

    fun listViewDisplayMode() = preferenceStore.getObject(
        "pref_list_view_display_mode",
        PosterDisplayMode.List,
        PosterDisplayMode.Serializer::serialize,
        PosterDisplayMode.Serializer::deserialize,
    )

    fun favoritesSortMode() = preferenceStore.getObject(
        "pref_favorites_sort_mode",
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

    fun listViewSortMode() = preferenceStore.getObject(
        "pref_list_view_sort_mode",
        ListSortMode.RecentlyAdded(true),
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

    fun librarySortMode() = preferenceStore.getObject(
        "pref_library_sort_mode",
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

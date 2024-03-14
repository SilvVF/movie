package io.silv.movie.presentation.library.view.favorite

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.GetFavoritesList
import io.silv.movie.data.prefrences.LibraryPreferences
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.data.prefrences.core.getOrDefault
import io.silv.movie.presentation.asState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesScreenModel(
    getFavoritesList: GetFavoritesList,
    private val libraryPreferences: LibraryPreferences
): ScreenModel {

    var listViewDisplayMode by libraryPreferences.listViewDisplayMode()
        .asState(screenModelScope)
        private set

    var currentDialog by mutableStateOf<Dialog?>(null)
        private set

    private val sortModeFavorites = libraryPreferences.sortModeFavorites()

    var query by mutableStateOf("")
        private set

    val state = snapshotFlow { query }
        .combine(
            sortModeFavorites.stateIn(screenModelScope)
        ) { a: String, b: FavoritesSortMode -> a to b }
        .flatMapLatest { (query, sortMode) ->
            getFavoritesList.subscribe(query, sortMode)
                .map { content ->
                    FavoritesListState(
                        sortMode,
                        content.toImmutableList()
                    )
                }
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            FavoritesListState(sortModeFavorites.getOrDefault())
        )

    fun setSortMode(sortMode: FavoritesSortMode) {
        screenModelScope.launch {
            sortModeFavorites.set(sortMode)
        }
    }

    fun updateQuery(q: String) {
        query = q
    }

    fun changeDialog(dialog: Dialog?) {
        screenModelScope.launch {
            currentDialog = dialog
        }
    }

    fun updateDisplayMode(displayMode: PosterDisplayMode) {
        listViewDisplayMode = displayMode
    }

    @Stable
    sealed interface Dialog {

        @Stable
        data object ListOptions : Dialog

        @Stable
        data class ContentOptions(val item: ContentItem): Dialog
    }
}

sealed interface FavoritesSortMode {
    data object Title: FavoritesSortMode
    data object RecentlyAdded: FavoritesSortMode
    data object Movie: FavoritesSortMode
    data object Show: FavoritesSortMode
}


data class FavoritesListState(
    val sortMode: FavoritesSortMode,
    val items: ImmutableList<ContentItem> = persistentListOf(),
)


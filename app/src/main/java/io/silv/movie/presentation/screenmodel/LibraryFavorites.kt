package io.silv.movie.presentation.screenmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.movie.core.Penta
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.RecommendationManager
import io.silv.movie.data.prefrences.LibraryPreferences
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.data.content.lists.ContentListRepository
import io.silv.movie.data.prefrences.core.getOrDefaultBlocking
import io.silv.movie.data.user.FavoritesUpdateManager
import io.silv.movie.presentation.asState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesScreenModel(
    private val recommendationManager: RecommendationManager,
    private val libraryPreferences: LibraryPreferences,
    private val contentListRepository: ContentListRepository,
    private val favoritesUpdateManager: FavoritesUpdateManager,
): ScreenModel {

    var listViewDisplayMode by libraryPreferences.listViewDisplayMode()
        .asState(screenModelScope)
        private set

    var currentDialog by mutableStateOf<Dialog?>(null)
        private set

    private val sortModeFavorites = libraryPreferences.favoritesSortMode()

    var query by mutableStateOf("")
        private set

    val state = combine(
            snapshotFlow { query },
            sortModeFavorites.stateIn(screenModelScope),
            recommendationManager.subscribe(-1L).map { it.take(6) },
            recommendationManager.isRunning(-1L),
            favoritesUpdateManager.isRunning(),
        ) { a, b, c, d, e -> Penta(a, b, c, d, e) }
        .flatMapLatest { (query, sortMode, recommendations, refRecs, refFavs) ->
            contentListRepository.observeFavorites(query, sortMode)
                .map { content ->
                    FavoritesListState(
                        sortMode,
                        content,
                        recommendations,
                        refRecs,
                        refFavs
                    )
                }
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            FavoritesListState(sortModeFavorites.getOrDefaultBlocking())
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

    fun refreshRecommendations() {
        screenModelScope.launch {
            recommendationManager.refreshFavoritesRecommendations()
        }
    }

    fun refreshFavoritesFromNetwork() {
        screenModelScope.launch {
            favoritesUpdateManager.refreshFavorites()
        }
    }


    @Stable
    sealed interface Dialog {

        @Stable
        data object ListOptions : Dialog

        @Stable
        data class ContentOptions(val item: ContentItem): Dialog

        @Stable
        data class RemoveFromFavorites(val item: ContentItem): Dialog
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
    val items: List<ContentItem> = emptyList(),
    val recommendations: List<ContentItem> = emptyList(),
    val refreshingRecommendations: Boolean = false,
    val refreshingFavorites: Boolean = false,
)


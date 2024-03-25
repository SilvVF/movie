package io.silv.movie.presentation.library.screenmodels

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.movie.core.Penta
import io.silv.movie.data.cache.MovieCoverCache
import io.silv.movie.data.cache.TVShowCoverCache
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.GetFavoritesList
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.movie.interactor.UpdateMovie
import io.silv.movie.data.movie.model.toMovieUpdate
import io.silv.movie.data.prefrences.LibraryPreferences
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.data.prefrences.core.getOrDefault
import io.silv.movie.data.recommendation.RecommendationManager
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.tv.interactor.UpdateShow
import io.silv.movie.data.tv.model.toShowUpdate
import io.silv.movie.data.user.FavoritesUpdateManager
import io.silv.movie.data.user.ListRepository
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
    private val recommendationManager: RecommendationManager,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
    private val updateShow: UpdateShow,
    private val updateMovie: UpdateMovie,
    private val tvCoverCache: TVShowCoverCache,
    private val movieCoverCache: MovieCoverCache,
    private val libraryPreferences: LibraryPreferences,
    private val favoritesUpdateManager: FavoritesUpdateManager,
    private val listRepository: ListRepository,
): ScreenModel {

    var listViewDisplayMode by libraryPreferences.listViewDisplayMode()
        .asState(screenModelScope)
        private set

    var currentDialog by mutableStateOf<Dialog?>(null)
        private set

    private val sortModeFavorites = libraryPreferences.sortModeFavorites()

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
            getFavoritesList.subscribe(query, sortMode)
                .map { content ->
                    FavoritesListState(
                        sortMode,
                        content.toImmutableList(),
                        recommendations.toImmutableList(),
                        refRecs,
                        refFavs
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

    fun toggleItemFavorite(contentItem: ContentItem) {
        screenModelScope.launch {
            if (contentItem.isMovie) {
                val movie = getMovie.await(contentItem.contentId) ?: return@launch

                val new = movie.copy(favorite = !movie.favorite)

                if(!new.favorite && !new.inList) {
                    movieCoverCache.deleteFromCache(movie)
                }
                updateMovie .await(new.toMovieUpdate())
            } else {
                val show = getShow.await(contentItem.contentId) ?: return@launch

                val new = show.copy(favorite = !show.favorite)

                if(!new.favorite && !new.inList) {
                    tvCoverCache.deleteFromCache(show)
                }
                updateShow.await(new.toShowUpdate())
            }
        }
    }

    fun addRecommendationToList(contentItem: ContentItem) {
        screenModelScope.launch {
            if (contentItem.isMovie) {
                val movie = getMovie.await(contentItem.contentId) ?: return@launch
                updateMovie.await(movie.copy(favorite = !movie.favorite).toMovieUpdate())
            } else {
                val show = getShow.await(contentItem.contentId) ?: return@launch
                updateShow.await(show.copy(favorite = !show.favorite).toShowUpdate())
            }
            recommendationManager.removeRecommendation(contentItem, -1)
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
    val items: ImmutableList<ContentItem> = persistentListOf(),
    val recommendations: ImmutableList<ContentItem> = persistentListOf(),
    val refreshingRecommendations: Boolean = false,
    val refreshingFavorites: Boolean = false,
)


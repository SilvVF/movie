package io.silv.movie.presentation.library.screens

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.PullRefresh
import io.silv.core_ui.components.topbar.rememberPosterTopBarState
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.LocalContentInteractor
import io.silv.movie.presentation.browse.components.RemoveEntryDialog
import io.silv.movie.presentation.library.components.ContentListPosterGrid
import io.silv.movie.presentation.library.components.ContentListPosterList
import io.silv.movie.presentation.library.components.dialog.FavoriteOptionsBottomSheet
import io.silv.movie.presentation.library.components.topbar.FavoritesViewTopBar
import io.silv.movie.presentation.library.screenmodels.FavoritesListState
import io.silv.movie.presentation.library.screenmodels.FavoritesScreenModel
import io.silv.movie.presentation.library.screenmodels.FavoritesSortMode
import io.silv.movie.presentation.view.movie.MovieViewScreen
import io.silv.movie.presentation.view.tv.TVViewScreen

data object FavoritesViewScreen : Screen {

    @Composable
    override fun Content() {

        val contentInteractor = LocalContentInteractor.current
        val screenModel = getScreenModel<FavoritesScreenModel>()
        val state by screenModel.state.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.currentOrThrow
        val changeDialog =
            remember { { dialog: FavoritesScreenModel.Dialog? -> screenModel.changeDialog(dialog) } }
        val toggleItemFavorite = remember {
            { contentItem: ContentItem -> contentInteractor.toggleFavorite(contentItem) }
        }

        FavoritesScreenContent(
            updateQuery = screenModel::updateQuery,
            onListOptionClick = { changeDialog(FavoritesScreenModel.Dialog.ListOptions) },
            listViewDisplayMode = { screenModel.listViewDisplayMode },
            updateListViewDisplayMode = screenModel::updateDisplayMode,
            query = screenModel.query,
            onClick = { item ->
                if (item.isMovie)
                    navigator.push(MovieViewScreen(item.contentId))
                else
                    navigator.push(TVViewScreen(item.contentId))
            },
            onLongClick = { item ->
                if (item.favorite) {
                    changeDialog(FavoritesScreenModel.Dialog.RemoveFromFavorites(item))
                } else {
                    toggleItemFavorite(item)
                }
            },
            onOptionsClick = {},
            changeSortMode = screenModel::setSortMode,
            refreshRecommendations = screenModel::refreshRecommendations,
            onRecommendationClick = { item ->
                if (item.isMovie)
                    navigator.push(MovieViewScreen(item.contentId))
                else
                    navigator.push(TVViewScreen(item.contentId))
            },
            onAddRecommendation = { contentInteractor.toggleFavorite(it) },
            onRecommendationLongClick = { contentInteractor.toggleFavorite(it) },
            refreshFavorites = screenModel::refreshFavoritesFromNetwork,
            state = state
        )
        val onDismissRequest = remember { { screenModel.changeDialog(null) } }
        when (val dialog = screenModel.currentDialog) {
            is FavoritesScreenModel.Dialog.ContentOptions -> {

            }
            FavoritesScreenModel.Dialog.ListOptions -> {
                FavoriteOptionsBottomSheet(
                    onDismissRequest = onDismissRequest,
                    onAddClick = { /*TODO*/ },
                    onShareClick = {}
                )
            }
            is FavoritesScreenModel.Dialog.RemoveFromFavorites -> {
                RemoveEntryDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = { toggleItemFavorite(dialog.item) },
                    entryToRemove = dialog.item.title
                )
            }
            null -> Unit
        }
    }
}

@Composable
private fun FavoritesScreenContent(
    updateQuery: (String) -> Unit,
    onListOptionClick:() -> Unit,
    listViewDisplayMode: () -> PosterDisplayMode,
    updateListViewDisplayMode: (PosterDisplayMode) -> Unit,
    query: String,
    onLongClick: (item: ContentItem) -> Unit,
    onClick: (item: ContentItem) -> Unit,
    onOptionsClick: (item: ContentItem) -> Unit,
    changeSortMode: (FavoritesSortMode) -> Unit,
    onRecommendationClick: (item: ContentItem) -> Unit,
    onRecommendationLongClick: (item: ContentItem) -> Unit,
    onAddRecommendation: (item: ContentItem) -> Unit,
    refreshRecommendations: () -> Unit,
    refreshFavorites: () -> Unit,
    state: FavoritesListState
) {
    val hazeState = remember { HazeState() }
    val topBarState = rememberPosterTopBarState()

    PullRefresh(
        refreshing = state.refreshingFavorites,
        enabled = { !state.refreshingFavorites },
        onRefresh = { refreshFavorites() }
    ) {
        Scaffold(
            topBar = {
                FavoritesViewTopBar(
                    state = topBarState,
                    query = { query },
                    changeQuery = updateQuery,
                    onSearch = updateQuery,
                    displayMode = listViewDisplayMode,
                    setDisplayMode = updateListViewDisplayMode,
                    onListOptionClicked = onListOptionClick,
                    sortModeProvider = { state.sortMode },
                    changeSortMode = changeSortMode,
                    modifier = Modifier.hazeChild(hazeState)
                )
            },
            modifier = Modifier
                .imePadding()
                .nestedScroll(topBarState.scrollBehavior.nestedScrollConnection)
        ) { paddingValues ->
            when (val mode = listViewDisplayMode()) {
                is PosterDisplayMode.Grid -> {
                    ContentListPosterGrid(
                        mode = mode,
                        items = state.items,
                        onOptionsClick = onOptionsClick,
                        onLongClick = onLongClick,
                        onClick = onClick,
                        showFavorite = false,
                        paddingValues = paddingValues,
                        recommendations = state.recommendations,
                        onRecommendationLongClick = onRecommendationLongClick,
                        onRecommendationClick = onRecommendationClick,
                        onAddRecommendation = onAddRecommendation,
                        refreshingRecommendations = state.refreshingRecommendations,
                        onRefreshClick = refreshRecommendations,
                        isOwnerMe = true,
                        modifier = Modifier
                            .haze(
                                state = hazeState,
                                style = HazeDefaults.style(MaterialTheme.colorScheme.background),
                            )
                            .padding(top = 12.dp),
                    )
                }

                PosterDisplayMode.List -> {
                    ContentListPosterList(
                        items = state.items,
                        onOptionsClick = onOptionsClick,
                        onLongClick = onLongClick,
                        onClick = onClick,
                        showFavorite = false,
                        paddingValues = paddingValues,
                        recommendations = state.recommendations,
                        onRecommendationLongClick = onRecommendationLongClick,
                        onRecommendationClick = onRecommendationClick,
                        onAddRecommendation = onAddRecommendation,
                        refreshingRecommendations = state.refreshingRecommendations,
                        onRefreshClick = refreshRecommendations,
                        isOwnerMe = true,
                        modifier = Modifier
                            .haze(
                                state = hazeState,
                                style = HazeDefaults.style(MaterialTheme.colorScheme.background),
                            )
                            .padding(top = 12.dp),
                    )
                }
            }
        }
    }
}
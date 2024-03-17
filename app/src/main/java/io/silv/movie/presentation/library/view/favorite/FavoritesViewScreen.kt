package io.silv.movie.presentation.library.view.favorite

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
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.topbar.rememberPosterTopBarState
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.library.components.ContentListPosterGrid
import io.silv.movie.presentation.library.components.ContentListPosterList
import kotlinx.collections.immutable.persistentListOf


object FavoritesViewScreen : Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<FavoritesScreenModel>()
        val state by screenModel.state.collectAsStateWithLifecycle()

        val changeDialog = remember { { dialog: FavoritesScreenModel.Dialog? -> screenModel.changeDialog(dialog) } }
        FavoritesScreenContent(
            updateQuery = screenModel::updateQuery,
            onListOptionClick = { changeDialog(FavoritesScreenModel.Dialog.ListOptions) },
            listViewDisplayMode = { screenModel.listViewDisplayMode },
            updateListViewDisplayMode = screenModel::updateDisplayMode,
            query = screenModel.query,
            onLongClick = {},
            onClick = {},
            onOptionsClick = {},
            changeSortMode = screenModel::setSortMode,
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
    state: FavoritesListState
) {
    val hazeState = remember { HazeState() }
    val topBarState = rememberPosterTopBarState()

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
                    recommendations = persistentListOf(),
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
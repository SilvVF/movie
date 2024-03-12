package io.silv.movie.presentation.library.view

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.GetFavoritesList
import io.silv.movie.data.prefrences.LibraryPreferences
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.asState
import io.silv.movie.presentation.library.components.FavoritesViewTopBar
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FavoritesScreenModel(
    getFavoritesList: GetFavoritesList,
    libraryPreferences: LibraryPreferences
): ScreenModel {

    var listViewDisplayMode by libraryPreferences.listViewDisplayMode().asState(screenModelScope)
        private set

    var query by mutableStateOf("")
        private set

    val state = snapshotFlow { query }
        .flatMapLatest {
            getFavoritesList.subscribe(it)
                .map { content ->
                    FavoritesListState(
                        content.toImmutableList()
                    )
                }
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            FavoritesListState()
        )

    fun updateQuery(q: String) {
        query = q
    }

    fun updateDisplayMode(displayMode: PosterDisplayMode) {
        listViewDisplayMode = displayMode
    }
}

object FavoritesViewScreen : Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<FavoritesScreenModel>()
        val state by screenModel.state.collectAsStateWithLifecycle()

        FavoritesScreenContent(
            updateQuery = screenModel::updateQuery,
            onListOptionClick = { /*TODO*/ },
            listViewDisplayMode = { screenModel.listViewDisplayMode },
            updateListViewDisplayMode = screenModel::updateDisplayMode,
            query = screenModel.query,
            onLongClick = {},
            onClick = {},
            onOptionsClick = {},
            state = state
        )
    }
}

data class FavoritesListState(
    val items: ImmutableList<ContentItem> = persistentListOf()
)

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
    state: FavoritesListState
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    Scaffold(
        topBar = {
            FavoritesViewTopBar(
                scrollBehavior = scrollBehavior,
                query = { query },
                changeQuery = updateQuery,
                onSearch = updateQuery,
                displayMode = listViewDisplayMode,
                setDisplayMode = updateListViewDisplayMode,
                onListOptionClicked = onListOptionClick,
                modifier = Modifier.hazeChild(hazeState)
            )
        },
        modifier = Modifier
            .imePadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
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
package io.silv.movie.presentation.library.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
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
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryComfortableGridItem
import io.silv.core_ui.components.EntryCompactGridItem
import io.silv.core_ui.components.EntryListItem
import io.silv.core_ui.components.FastScrollLazyColumn
import io.silv.core_ui.components.PosterData
import io.silv.core_ui.components.VerticalGridFastScroller
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.browse.movie.components.InLibraryBadge
import io.silv.movie.presentation.library.ListEditScreen
import io.silv.movie.presentation.library.components.ListViewTopBar
import io.silv.movie.presentation.toPoster
import kotlinx.collections.immutable.ImmutableList
import org.koin.core.parameter.parametersOf

data class ListViewScreen(
    private val listId: Long,
): Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<ListViewScreenModel> { parametersOf(listId) }
        val state by screenModel.state.collectAsStateWithLifecycle()

        when (val s = state) {
            is ListViewState.Error -> Unit
            ListViewState.Loading -> Unit
            is ListViewState.Success -> {

                val listEditScreen = remember(s.list.name) { ListEditScreen(s.list.name) }

                val screenResultLauncher = rememberScreenWithResultLauncher(
                    screen = listEditScreen
                ) { result ->
                    screenModel.editList(s.list, result.name)
                }

                SuccessScreenContent(
                    query = screenModel.query,
                    updateQuery = screenModel::updateQuery,
                    onListOptionClick = { screenResultLauncher.launch() },
                    updateListViewDisplayMode = screenModel::updateListViewDisplayMode,
                    listViewDisplayMode = { screenModel.listViewDisplayMode },
                    onClick = { },
                    onLongClick = { },
                    onOptionsClick = {},
                    state = s
                )
            }
        }
    }
}

@Composable
private fun SuccessScreenContent(
    updateQuery: (String) -> Unit,
    onListOptionClick:() -> Unit,
    listViewDisplayMode: () -> PosterDisplayMode,
    updateListViewDisplayMode: (PosterDisplayMode) -> Unit,
    query: String,
    onLongClick: (item: ContentItem) -> Unit,
    onClick: (item: ContentItem) -> Unit,
    onOptionsClick: (item: ContentItem) -> Unit,
    state: ListViewState.Success
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        topBar = {
            ListViewTopBar(
                scrollBehavior = scrollBehavior,
                query = { query },
                changeQuery = updateQuery,
                onSearch = updateQuery,
                items = { state.allItems },
                contentListProvider = { state.list },
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

@Composable
fun ContentItemSourceCoverOnlyGridItem(
    favorite: Boolean,
    poster: PosterData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    EntryCompactGridItem(
        title = null,
        coverData = poster,
        coverAlpha = if (favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = favorite) },
        onLongClick = { onLongClick() },
        onClick = { onClick() },
    )
}

@Composable
fun ContentItemCompactGridItem(
    title: String,
    favorite: Boolean,
    poster: PosterData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    EntryCompactGridItem(
        title = title,
        coverData = poster,
        coverAlpha = if (favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = favorite) },
        onLongClick = { onLongClick() },
        onClick = { onClick() },
    )
}

@Composable
fun ContentItemComfortableGridItem(
    title: String,
    favorite: Boolean,
    poster: PosterData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    EntryComfortableGridItem(
        title = title,
        coverData = poster,
        coverAlpha = if (favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = favorite) },
        onLongClick = { onLongClick() },
        onClick = { onClick() },
    )
}

@Composable
fun ContentListPosterGrid(
    items: ImmutableList<ContentItem>,
    mode: PosterDisplayMode.Grid,
    paddingValues: PaddingValues,
    onLongClick: (item: ContentItem) -> Unit,
    onClick: (item: ContentItem) -> Unit,
    onOptionsClick: (item: ContentItem) -> Unit,
    modifier: Modifier = Modifier,
    showFavorite: Boolean = true
) {
    val gridState = rememberLazyGridState()
    val cols = GridCells.Fixed(2)

    VerticalGridFastScroller(
        state = gridState,
        columns = cols,
        contentPadding = paddingValues,
        arrangement = Arrangement.SpaceEvenly
    ) {
        LazyVerticalGrid(
            columns = cols,
            state = gridState,
            contentPadding = paddingValues,
            modifier = modifier,
        ) {
            items(items, { it.contentId }) {
                val poster = remember(it) { it.toPoster() }
                val favorite = showFavorite && it.favorite
                when (mode) {
                    PosterDisplayMode.Grid.ComfortableGrid -> {
                        ContentItemComfortableGridItem(
                            title = it.title,
                            favorite = favorite,
                            poster = poster,
                            onClick = { onClick(it) },
                            onLongClick = { onLongClick(it) },
                            onOptionsClick = { onOptionsClick(it) }
                        )
                    }
                    PosterDisplayMode.Grid.CompactGrid -> {
                        ContentItemCompactGridItem(
                            title = it.title,
                            favorite = favorite,
                            poster = poster,
                            onClick = { onClick(it) },
                            onLongClick = { onLongClick(it) },
                            onOptionsClick = { onOptionsClick(it) }
                        )
                    }
                    PosterDisplayMode.Grid.CoverOnlyGrid -> {
                        ContentItemSourceCoverOnlyGridItem(
                            favorite = favorite,
                            poster = poster,
                            onClick = { onClick(it) },
                            onLongClick = { onLongClick(it) },
                            onOptionsClick = { onOptionsClick(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContentListPosterList(
    items: ImmutableList<ContentItem>,
    paddingValues: PaddingValues,
    onLongClick: (item: ContentItem) -> Unit,
    onClick: (item: ContentItem) -> Unit,
    onOptionsClick: (item: ContentItem) -> Unit,
    modifier: Modifier = Modifier,
    showFavorite: Boolean = true
) {
    FastScrollLazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = paddingValues
    ) {
        items(items, { it.contentId }) {
            ContentListItem(
                title = it.title,
                favorite = showFavorite && it.favorite,
                poster = remember(it) { it.toPoster() },
                onClick = { onClick(it) },
                onLongClick = { onLongClick(it) },
                onOptionsClick = { onOptionsClick(it) }
            )
        }
    }
}

@Composable
private fun ContentListItem(
    title: String,
    favorite: Boolean,
    poster: PosterData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    EntryListItem(
        title = title,
        coverData = poster,
        coverAlpha = if (favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        badge = {
            InLibraryBadge(enabled = favorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
        endButton = {
            IconButton(
                onClick = onOptionsClick
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Options",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    )
}
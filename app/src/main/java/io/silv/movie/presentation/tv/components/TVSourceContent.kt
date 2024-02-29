package io.silv.movie.presentation.tv.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.core_ui.components.loadingIndicatorItem
import io.silv.core_ui.util.isScrollingUp
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.data.tv.TVShow
import io.silv.movie.presentation.LocalIsScrolling
import io.silv.movie.presentation.tv.browse.TVActions
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TVSourcePagingContent(
    paddingValues: PaddingValues,
    snackbarHostState: SnackbarHostState,
    actions: TVActions,
    displayMode: () -> PosterDisplayMode,
    pagingFlowFlow: () -> StateFlow<PagingData<StateFlow<TVShow>>>,
    gridCellsCount: () -> Int,
    modifier: Modifier = Modifier,
) {
    val pagingItems = pagingFlowFlow().collectAsLazyPagingItems()

    LaunchedEffect(pagingItems) {
        snapshotFlow { pagingItems.loadState.append }.collectLatest { loadState ->
            when(loadState) {
                is LoadState.Error -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Failed to load items",
                        withDismissAction = false,
                        actionLabel = "Retry",
                        duration = SnackbarDuration.Indefinite
                    )
                    when (result) {
                        SnackbarResult.ActionPerformed -> pagingItems.retry()
                        else -> Unit
                    }
                }
                else -> Unit
            }
        }
    }
    if (pagingItems.loadState.refresh is LoadState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (pagingItems.loadState.refresh is LoadState.Error) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("error")
        }
        return
    }

    when (val mode = displayMode()) {
        is PosterDisplayMode.Grid -> {
            TVSourcePosterGrid(
                mode = mode,
                modifier = modifier,
                gridCellsCount = gridCellsCount,
                paddingValues = paddingValues,
                pagingItems = pagingItems,
                onShowClick =  actions.showClick,
                onShowLongClick = actions.showLongClick
            )
        }
        PosterDisplayMode.List -> {
            BrowseTVSourceList(
                modifier = modifier,
                pagingItems = pagingItems,
                contentPadding = paddingValues,
                onShowClick =  actions.showClick,
                onShowLongClick = actions.showLongClick
            )
        }
    }
}

@Composable
fun TVSourcePosterGrid(
    mode: PosterDisplayMode.Grid,
    modifier: Modifier,
    gridCellsCount:() -> Int,
    paddingValues: PaddingValues,
    pagingItems: LazyPagingItems<StateFlow<TVShow>>,
    onShowClick: (TVShow) -> Unit,
    onShowLongClick: (TVShow) -> Unit,
) {
    val gridState = rememberLazyGridState()

    var isScrolling by LocalIsScrolling.current
    val isScrollingUp = gridState.isScrollingUp()

    LaunchedEffect(isScrollingUp) {
        isScrolling = isScrollingUp
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridCellsCount()),
        contentPadding = paddingValues,
        state = gridState,
        modifier = modifier.padding(top = 12.dp)
    ) {
        items(
            key = pagingItems.itemKey { it.value.id },
            count = pagingItems.itemCount,
            contentType = pagingItems.itemContentType { mode.hashCode() }
        ) {

            val tvShow by pagingItems[it]?.collectAsStateWithLifecycle() ?: return@items


            when(mode) {
                PosterDisplayMode.Grid.ComfortableGrid -> {
                    BrowseShowSourceComfortableGridItem(
                        show = tvShow,
                        onClick = onShowClick,
                        onLongClick = onShowLongClick
                    )
                }
                PosterDisplayMode.Grid.CompactGrid -> {
                    BrowsShowSourceCompactGridItem(
                        show = tvShow,
                        onClick = onShowClick,
                        onLongClick = onShowLongClick
                    )
                }
                PosterDisplayMode.Grid.CoverOnlyGrid -> {
                    BrowseShowSourceCoverOnlyGridItem(
                        show = tvShow,
                        onClick = onShowClick,
                        onLongClick = onShowLongClick
                    )
                }
            }
        }
        loadingIndicatorItem(pagingItems)
    }
}
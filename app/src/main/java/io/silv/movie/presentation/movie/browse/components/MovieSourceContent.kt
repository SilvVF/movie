package io.silv.movie.presentation.movie.browse.components

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
import io.silv.movie.data.movie.model.Movie
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.movie.browse.MovieActions
import io.silv.movie.presentation.movie.browse.MovieScreen
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest


@Composable
fun MovieSourcePagingContent(
    paddingValues: PaddingValues,
    snackbarHostState: SnackbarHostState,
    actions: MovieActions,
    displayMode: () -> PosterDisplayMode,
    pagingFlowFlow: () -> StateFlow<PagingData<StateFlow<Movie>>>,
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
           MovieSourcePosterGrid(
               mode = mode,
               modifier = modifier,
               gridCellsCount = gridCellsCount,
               paddingValues = paddingValues,
               pagingItems = pagingItems,
               onMovieClick =  actions.movieClick,
               onMovieLongClick = actions.movieLongClick
           )
        }
        PosterDisplayMode.List -> {
            BrowseMovieSourceList(
                modifier = modifier,
                pagingItems = pagingItems,
                contentPadding = paddingValues,
                onMovieClick = actions.movieClick,
                onMovieLongClick = actions.movieLongClick
            )
        }
    }
}

@Composable
fun MovieSourcePosterGrid(
    mode: PosterDisplayMode.Grid,
    modifier: Modifier,
    gridCellsCount:() -> Int,
    paddingValues: PaddingValues,
    pagingItems: LazyPagingItems<StateFlow<Movie>>,
    onMovieClick: (Movie) -> Unit,
    onMovieLongClick: (Movie) -> Unit,
) {
    val gridState = rememberLazyGridState()

    var isScrolling by MovieScreen.LocalIsScrolling.current
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

            val movie by pagingItems[it]?.collectAsStateWithLifecycle() ?: return@items


            when(mode) {
                PosterDisplayMode.Grid.ComfortableGrid -> {
                    BrowseMovieSourceComfortableGridItem(
                        movie = movie,
                        onClick = onMovieClick,
                        onLongClick = onMovieLongClick
                    )
                }
                PosterDisplayMode.Grid.CompactGrid -> {
                    BrowseMovieSourceCompactGridItem(
                        movie = movie,
                        onClick = onMovieClick,
                        onLongClick = onMovieLongClick
                    )
                }
                PosterDisplayMode.Grid.CoverOnlyGrid -> {
                    BrowseMovieSourceCoverOnlyGridItem(
                        movie = movie,
                        onClick = onMovieClick,
                        onLongClick = onMovieLongClick
                    )
                }
            }
        }
        loadingIndicatorItem(pagingItems)
    }
}
package io.silv.movie.presentation.movie.discover.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.core_ui.components.loadingIndicatorItem
import io.silv.data.movie.model.Movie
import io.silv.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.movie.browse.components.BrowseMovieSourceComfortableGridItem
import io.silv.movie.presentation.movie.browse.components.BrowseMovieSourceCompactGridItem
import io.silv.movie.presentation.movie.browse.components.BrowseMovieSourceCoverOnlyGridItem
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SelectedPagingItemsGrid(
    mode: PosterDisplayMode.Grid,
    modifier: Modifier,
    gridCellsCount:() -> Int,
    paddingValues: PaddingValues,
    pagingItems: LazyPagingItems<StateFlow<Movie>>,
    onMovieClick: (Movie) -> Unit,
    onMovieLongClick: (Movie) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridCellsCount()),
        contentPadding = paddingValues,
        modifier = modifier.padding(top = 12.dp)
    ) {
        items(
            key = pagingItems.itemKey { it.value.id },
            count = pagingItems.itemCount,
            contentType = pagingItems.itemContentType { mode.hashCode() }
        ) {

            val movie = pagingItems[it]

            movie?.let {
                when(mode) {
                    PosterDisplayMode.Grid.ComfortableGrid -> {
                        BrowseMovieSourceComfortableGridItem(
                            movieFlow = movie,
                            onClick = onMovieClick,
                            onLongClick = onMovieLongClick
                        )
                    }
                    PosterDisplayMode.Grid.CompactGrid -> {
                        BrowseMovieSourceCompactGridItem(
                            movieFlow = movie,
                            onClick = onMovieClick,
                            onLongClick = onMovieLongClick
                        )
                    }
                    PosterDisplayMode.Grid.CoverOnlyGrid -> {
                        BrowseMovieSourceCoverOnlyGridItem(
                            movieFlow = movie,
                            onClick = onMovieClick,
                            onLongClick = onMovieLongClick
                        )
                    }
                }
            }
        }
        loadingIndicatorItem(pagingItems)
    }
}
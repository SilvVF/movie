package io.silv.movie.presentation.movie.discover.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.core_ui.components.loadingIndicatorItem
import io.silv.data.movie.model.Movie
import io.silv.data.movie.model.TVShow
import io.silv.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.movie.browse.components.BrowsShowSourceCompactGridItem
import io.silv.movie.presentation.movie.browse.components.BrowseMovieSourceComfortableGridItem
import io.silv.movie.presentation.movie.browse.components.BrowseMovieSourceCompactGridItem
import io.silv.movie.presentation.movie.browse.components.BrowseMovieSourceCoverOnlyGridItem
import io.silv.movie.presentation.movie.browse.components.BrowseShowSourceComfortableGridItem
import io.silv.movie.presentation.movie.browse.components.BrowseShowSourceCoverOnlyGridItem
import io.silv.movie.presentation.movie.discover.Content
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SelectedPagingItemsGrid(
    mode: PosterDisplayMode.Grid,
    modifier: Modifier,
    gridCellsCount:() -> Int,
    paddingValues: PaddingValues,
    pagingItems: LazyPagingItems<StateFlow<Content>>,
    onMovieClick: (Movie) -> Unit,
    onMovieLongClick: (Movie) -> Unit,
    onShowLongClick: (TVShow) -> Unit,
    onShowClick: (TVShow) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridCellsCount()),
        contentPadding = paddingValues,
        modifier = modifier.padding(top = 12.dp)
    ) {
        items(
            key = pagingItems.itemKey { when(val value = it.value) {
                is Content.CMovie -> value.movie.id
                is Content.CShow -> value.show.id
            } },
            count = pagingItems.itemCount,
            contentType = pagingItems.itemContentType { mode.hashCode() }
        ) {

            val content by pagingItems[it]?.collectAsStateWithLifecycle() ?: return@items

           when(val c = content) {
               is Content.CMovie -> {
                   when(mode) {
                       PosterDisplayMode.Grid.ComfortableGrid -> {
                           BrowseMovieSourceComfortableGridItem(
                               movie = c.movie,
                               onClick = onMovieClick,
                               onLongClick = onMovieLongClick
                           )
                       }
                       PosterDisplayMode.Grid.CompactGrid -> {
                           BrowseMovieSourceCompactGridItem(
                               movie = c.movie,
                               onClick = onMovieClick,
                               onLongClick = onMovieLongClick
                           )
                       }
                       PosterDisplayMode.Grid.CoverOnlyGrid -> {
                           BrowseMovieSourceCoverOnlyGridItem(
                               movie = c.movie,
                               onClick = onMovieClick,
                               onLongClick = onMovieLongClick
                           )
                       }
                   }
               }
               is Content.CShow -> {
                   when(mode) {
                       PosterDisplayMode.Grid.ComfortableGrid -> {
                           BrowseShowSourceComfortableGridItem(
                               show = c.show,
                               onClick = onShowClick,
                               onLongClick = onShowLongClick
                           )
                       }
                       PosterDisplayMode.Grid.CompactGrid -> {
                           BrowsShowSourceCompactGridItem(
                               show = c.show,
                               onClick = onShowClick,
                               onLongClick = onShowLongClick
                           )
                       }
                       PosterDisplayMode.Grid.CoverOnlyGrid -> {
                           BrowseShowSourceCoverOnlyGridItem(
                               show = c.show,
                               onClick = onShowClick,
                               onLongClick = onShowLongClick
                           )
                       }
                   }
               }
           }
        }
        loadingIndicatorItem(pagingItems)
    }
}
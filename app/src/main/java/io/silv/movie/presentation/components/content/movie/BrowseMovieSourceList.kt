package io.silv.movie.presentation.components.content.movie

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryListItem
import io.silv.core_ui.components.PageLoadingIndicator
import io.silv.core_ui.util.isScrollingUp
import io.silv.core_ui.util.plus
import io.silv.movie.data.content.movie.model.MoviePoster
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.LocalIsScrolling
import io.silv.movie.presentation.tabs.SharedElement
import io.silv.movie.presentation.tabs.registerSharedElement
import io.silv.movie.presentation.toPoster
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BrowseMovieSourceList(
    modifier: Modifier,
    pagingItems: LazyPagingItems<StateFlow<MoviePoster>>,
    contentPadding: PaddingValues,
    onMovieClick: (MoviePoster) -> Unit,
    onMovieLongClick: (MoviePoster) -> Unit,
) {
    val listState = rememberLazyListState()

    var isScrolling by LocalIsScrolling.current
    val isScrollingUp = listState.isScrollingUp()

    LaunchedEffect(isScrollingUp) {
        isScrolling = isScrollingUp
    }


    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
    ) {
        items(
            count = pagingItems.itemCount,
            contentType = pagingItems.itemContentType { PosterDisplayMode.List.hashCode() },
            key = pagingItems.itemKey { it.value.id }
        ) { index ->
            val movie by pagingItems[index]?.collectAsStateWithLifecycle() ?: return@items

            BrowseMovieSourceListItem(
                movie = movie,
                onClick = { onMovieClick(movie) },
                onLongClick = { onMovieLongClick(movie) }
            )
        }

        if (pagingItems.loadState.append is LoadState.Loading) {
            item(
                key = "loading-append"
            ) {
                PageLoadingIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BrowseMovieSourceListItem(
    movie: MoviePoster,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    EntryListItem(
        title = movie.title,
        coverData = movie.toPoster(),
        coverAlpha = if (movie.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        badge = {
            InLibraryBadge(enabled = movie.favorite)
        },
        coverModifier = Modifier.registerSharedElement(SharedElement.Movie(movie.id)),
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
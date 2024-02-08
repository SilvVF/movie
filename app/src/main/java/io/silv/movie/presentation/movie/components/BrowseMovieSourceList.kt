package io.silv.movie.presentation.movie.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import io.silv.core_ui.components.toPoster
import io.silv.core_ui.util.plus
import io.silv.data.Movie
import io.silv.data.prefrences.PosterDisplayMode
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BrowseMovieSourceList(
    modifier: Modifier,
    pagingItems: LazyPagingItems<StateFlow<Movie>>,
    contentPadding: PaddingValues,
    onMovieClick: (Movie) -> Unit,
    onMovieLongClick: (Movie) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
    ) {
        items(
            count = pagingItems.itemCount,
            contentType = pagingItems.itemContentType { PosterDisplayMode.List.hashCode() },
            key = pagingItems.itemKey { it.value.id }
        ) { index ->
            val movie by pagingItems[index]?.collectAsStateWithLifecycle() ?: return@items

            BrowseMangaSourceListItem(
                movie = movie,
                onClick = { onMovieClick(movie) },
                onLongClick = { onMovieLongClick(movie) },
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
private fun BrowseMangaSourceListItem(
    movie: Movie,
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
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
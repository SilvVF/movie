package io.silv.movie.presentation.movie.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.silv.core_ui.components.Badge
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryComfortableGridItem
import io.silv.core_ui.components.EntryCompactGridItem
import io.silv.core_ui.components.toPoster
import io.silv.data.Movie
import kotlinx.coroutines.flow.StateFlow

@Composable
fun InLibraryBadge(enabled: Boolean) {
    if (enabled) {
        Badge(imageVector = Icons.Outlined.CollectionsBookmark,)
    }
}


@Composable
fun BrowseMovieSourceCoverOnlyGridItem(
    movieFlow: StateFlow<Movie>,
    onClick: (Movie) -> Unit = {},
    onLongClick: (Movie) -> Unit = {},
) {
    val movie by movieFlow.collectAsStateWithLifecycle()
    EntryCompactGridItem(
        title = null,
        coverData = movie.toPoster(),
        coverAlpha = if (movie.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = movie.favorite) },
        onLongClick = { onLongClick(movie) },
        onClick = { onClick(movie) },
    )
}

@Composable
fun BrowseMovieSourceCompactGridItem(
    movieFlow: StateFlow<Movie>,
    onClick: (Movie) -> Unit = {},
    onLongClick: (Movie) -> Unit = {},
) {
    val movie by movieFlow.collectAsStateWithLifecycle()
    EntryCompactGridItem(
        title = movie.title,
        coverData = movie.toPoster(),
        coverAlpha = if (movie.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = movie.favorite) },
        onLongClick = { onLongClick(movie) },
        onClick = { onClick(movie) },
    )
}

@Composable
fun BrowseMovieSourceComfortableGridItem(
    movieFlow: StateFlow<Movie>,
    onClick: (Movie) -> Unit = {},
    onLongClick: (Movie) -> Unit = {},
) {
    val movie by movieFlow.collectAsStateWithLifecycle()

    EntryComfortableGridItem(
        title = movie.title,
        coverData = movie.toPoster(),
        coverAlpha = if (movie.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = movie.favorite) },
        onLongClick = { onLongClick(movie) },
        onClick = { onClick(movie) },
    )
}

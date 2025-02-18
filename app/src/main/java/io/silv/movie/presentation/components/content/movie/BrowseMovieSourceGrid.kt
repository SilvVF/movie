package io.silv.movie.presentation.components.content.movie

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.runtime.Composable
import io.silv.core_ui.components.Badge
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryComfortableGridItem
import io.silv.core_ui.components.EntryCompactGridItem
import io.silv.movie.data.model.ContentItem
import io.silv.movie.data.model.MoviePoster
import io.silv.movie.presentation.toPoster

@Composable
fun InLibraryBadge(enabled: Boolean) {
    if (enabled) {
        Badge(imageVector = Icons.Outlined.CollectionsBookmark,)
    }
}


@Composable
fun BrowseMovieSourceCoverOnlyGridItem(
    movie: ContentItem,
    onClick: (ContentItem) -> Unit = {},
    onLongClick: (ContentItem) -> Unit = {},
) {
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
    movie: ContentItem,
    onClick: (ContentItem) -> Unit = {},
    onLongClick: (ContentItem) -> Unit = {},
) {
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
    movie: ContentItem,
    onClick: (ContentItem) -> Unit = {},
    onLongClick: (ContentItem) -> Unit = {},
) {
    EntryComfortableGridItem(
        title = movie.title,
        coverData = movie.toPoster(),
        coverAlpha = if (movie.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = movie.favorite) },
        onLongClick = { onLongClick(movie) },
        onClick = { onClick(movie) },
    )
}

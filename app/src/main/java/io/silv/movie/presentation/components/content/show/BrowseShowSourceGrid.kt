package io.silv.movie.presentation.components.content.show

import androidx.compose.runtime.Composable
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryComfortableGridItem
import io.silv.core_ui.components.EntryCompactGridItem
import io.silv.movie.data.model.ContentItem
import io.silv.movie.data.model.TVShowPoster
import io.silv.movie.presentation.components.content.movie.InLibraryBadge
import io.silv.movie.presentation.toPoster

@Composable
fun BrowseShowSourceCoverOnlyGridItem(
    show: ContentItem,
    onClick: (ContentItem) -> Unit = {},
    onLongClick: (ContentItem) -> Unit = {},
) {
    EntryCompactGridItem(
        title = null,
        coverData = show.toPoster(),
        coverAlpha = if (show.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = show.favorite) },
        onLongClick = { onLongClick(show) },
        onClick = { onClick(show) },
    )
}

@Composable
fun BrowsShowSourceCompactGridItem(
    show: ContentItem,
    onClick: (ContentItem) -> Unit = {},
    onLongClick: (ContentItem) -> Unit = {},
) {
    EntryCompactGridItem(
        title = show.title,
        coverData = show.toPoster(),
        coverAlpha = if (show.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = show.favorite) },
        onLongClick = { onLongClick(show) },
        onClick = { onClick(show) },
    )
}

@Composable
fun BrowseShowSourceComfortableGridItem(
    show: ContentItem,
    onClick: (ContentItem) -> Unit = {},
    onLongClick: (ContentItem) -> Unit = {},
) {
    EntryComfortableGridItem(
        title = show.title,
        coverData = show.toPoster(),
        coverAlpha = if (show.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = show.favorite) },
        onLongClick = { onLongClick(show) },
        onClick = { onClick(show) },
    )
}
package io.silv.movie.presentation.tv.components

import androidx.compose.runtime.Composable
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryComfortableGridItem
import io.silv.core_ui.components.EntryCompactGridItem
import io.silv.movie.data.tv.TVShow
import io.silv.movie.presentation.movie.browse.components.InLibraryBadge
import io.silv.movie.presentation.toPoster

@Composable
fun BrowseShowSourceCoverOnlyGridItem(
    show: TVShow,
    onClick: (TVShow) -> Unit = {},
    onLongClick: (TVShow) -> Unit = {},
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
    show: TVShow,
    onClick: (TVShow) -> Unit = {},
    onLongClick: (TVShow) -> Unit = {},
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
    show: TVShow,
    onClick: (TVShow) -> Unit = {},
    onLongClick: (TVShow) -> Unit = {},
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
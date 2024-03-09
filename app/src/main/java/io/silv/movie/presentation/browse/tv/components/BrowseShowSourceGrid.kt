package io.silv.movie.presentation.browse.tv.components

import androidx.compose.runtime.Composable
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryComfortableGridItem
import io.silv.core_ui.components.EntryCompactGridItem
import io.silv.movie.data.tv.model.TVShowPoster
import io.silv.movie.presentation.browse.movie.components.InLibraryBadge
import io.silv.movie.presentation.toPoster

@Composable
fun BrowseShowSourceCoverOnlyGridItem(
    show: TVShowPoster,
    onClick: (TVShowPoster) -> Unit = {},
    onLongClick: (TVShowPoster) -> Unit = {},
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
    show: TVShowPoster,
    onClick: (TVShowPoster) -> Unit = {},
    onLongClick: (TVShowPoster) -> Unit = {},
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
    show: TVShowPoster,
    onClick: (TVShowPoster) -> Unit = {},
    onLongClick: (TVShowPoster) -> Unit = {},
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
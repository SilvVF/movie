package io.silv.movie.presentation.movie.browse.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.runtime.Composable
import io.silv.core_ui.components.Badge
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryComfortableGridItem
import io.silv.core_ui.components.EntryCompactGridItem
import io.silv.core_ui.components.toPoster
import io.silv.data.movie.model.Movie
import io.silv.data.tv.TVShow

@Composable
fun InLibraryBadge(enabled: Boolean) {
    if (enabled) {
        Badge(imageVector = Icons.Outlined.CollectionsBookmark,)
    }
}

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

@Composable
fun BrowseMovieSourceCoverOnlyGridItem(
    movie: Movie,
    onClick: (Movie) -> Unit = {},
    onLongClick: (Movie) -> Unit = {},
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
    movie: Movie,
    onClick: (Movie) -> Unit = {},
    onLongClick: (Movie) -> Unit = {},
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
    movie: Movie,
    onClick: (Movie) -> Unit = {},
    onLongClick: (Movie) -> Unit = {},
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

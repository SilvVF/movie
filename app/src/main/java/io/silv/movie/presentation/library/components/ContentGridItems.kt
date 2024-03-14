package io.silv.movie.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryComfortableGridItem
import io.silv.core_ui.components.EntryCompactGridItem
import io.silv.core_ui.components.PosterData
import io.silv.core_ui.components.lazy.VerticalGridFastScroller
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.browse.movie.components.InLibraryBadge
import io.silv.movie.presentation.toPoster
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ContentItemSourceCoverOnlyGridItem(
    favorite: Boolean,
    poster: PosterData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    EntryCompactGridItem(
        title = null,
        coverData = poster,
        coverAlpha = if (favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = favorite) },
        onLongClick = { onLongClick() },
        onClick = { onClick() },
    )
}

@Composable
fun ContentItemCompactGridItem(
    title: String,
    favorite: Boolean,
    poster: PosterData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    EntryCompactGridItem(
        title = title,
        coverData = poster,
        coverAlpha = if (favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = favorite) },
        onLongClick = { onLongClick() },
        onClick = { onClick() },
    )
}

@Composable
fun ContentItemComfortableGridItem(
    title: String,
    favorite: Boolean,
    poster: PosterData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    EntryComfortableGridItem(
        title = title,
        coverData = poster,
        coverAlpha = if (favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = favorite) },
        onLongClick = { onLongClick() },
        onClick = { onClick() },
    )
}

@Composable
fun ContentListPosterGrid(
    items: ImmutableList<ContentItem>,
    mode: PosterDisplayMode.Grid,
    paddingValues: PaddingValues,
    onLongClick: (item: ContentItem) -> Unit,
    onClick: (item: ContentItem) -> Unit,
    onOptionsClick: (item: ContentItem) -> Unit,
    modifier: Modifier = Modifier,
    showFavorite: Boolean = true
) {
    val gridState = rememberLazyGridState()
    val cols = GridCells.Fixed(2)

    VerticalGridFastScroller(
        state = gridState,
        columns = cols,
        contentPadding = paddingValues,
        arrangement = Arrangement.SpaceEvenly
    ) {
        LazyVerticalGrid(
            columns = cols,
            state = gridState,
            contentPadding = paddingValues,
            modifier = modifier,
        ) {
            items(items, { it.contentId }) {
                val poster = remember(it) { it.toPoster() }
                val favorite = showFavorite && it.favorite
                when (mode) {
                    PosterDisplayMode.Grid.ComfortableGrid -> {
                        ContentItemComfortableGridItem(
                            title = it.title,
                            favorite = favorite,
                            poster = poster,
                            onClick = { onClick(it) },
                            onLongClick = { onLongClick(it) },
                            onOptionsClick = { onOptionsClick(it) }
                        )
                    }
                    PosterDisplayMode.Grid.CompactGrid -> {
                        ContentItemCompactGridItem(
                            title = it.title,
                            favorite = favorite,
                            poster = poster,
                            onClick = { onClick(it) },
                            onLongClick = { onLongClick(it) },
                            onOptionsClick = { onOptionsClick(it) }
                        )
                    }
                    PosterDisplayMode.Grid.CoverOnlyGrid -> {
                        ContentItemSourceCoverOnlyGridItem(
                            favorite = favorite,
                            poster = poster,
                            onClick = { onClick(it) },
                            onLongClick = { onLongClick(it) },
                            onOptionsClick = { onOptionsClick(it) }
                        )
                    }
                }
            }
        }
    }
}

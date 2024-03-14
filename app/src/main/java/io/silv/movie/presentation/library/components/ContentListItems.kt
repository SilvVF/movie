package io.silv.movie.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryListItem
import io.silv.core_ui.components.lazy.FastScrollLazyColumn
import io.silv.core_ui.components.PosterData
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.presentation.browse.movie.components.InLibraryBadge
import io.silv.movie.presentation.toPoster
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ContentListPosterList(
    items: ImmutableList<ContentItem>,
    paddingValues: PaddingValues,
    onLongClick: (item: ContentItem) -> Unit,
    onClick: (item: ContentItem) -> Unit,
    onOptionsClick: (item: ContentItem) -> Unit,
    modifier: Modifier = Modifier,
    showFavorite: Boolean = true
) {
    FastScrollLazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = paddingValues
    ) {
        items(items, { it.contentId }) {
            ContentListItem(
                title = it.title,
                favorite = showFavorite && it.favorite,
                poster = remember(it) { it.toPoster() },
                onClick = { onClick(it) },
                onLongClick = { onLongClick(it) },
                onOptionsClick = { onOptionsClick(it) }
            )
        }
    }
}

@Composable
private fun ContentListItem(
    title: String,
    favorite: Boolean,
    poster: PosterData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    EntryListItem(
        title = title,
        coverData = poster,
        coverAlpha = if (favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        badge = {
            InLibraryBadge(enabled = favorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
        endButton = {
            IconButton(
                onClick = onOptionsClick
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Options",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    )
}
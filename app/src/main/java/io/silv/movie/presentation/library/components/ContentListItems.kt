package io.silv.movie.presentation.library.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryListItem
import io.silv.core_ui.components.PosterData
import io.silv.core_ui.components.lazy.FastScrollLazyColumn
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.presentation.browse.movie.components.InLibraryBadge
import io.silv.movie.presentation.toPoster
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ContentListPosterList(
    items: ImmutableList<ContentItem>,
    paddingValues: PaddingValues,
    onLongClick: (item: ContentItem) -> Unit,
    onClick: (item: ContentItem) -> Unit,
    onOptionsClick: (item: ContentItem) -> Unit,
    modifier: Modifier = Modifier,
    recommendations: ImmutableList<ContentItem> = persistentListOf(),
    refreshingRecommendations: Boolean = false,
    onRefreshClick: () -> Unit = {},
    showFavorite: Boolean = true,
) {
    FastScrollLazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = paddingValues
    ) {
        items(items, { "${it.contentId}${it.isMovie}"}) {
            ContentListItem(
                title = it.title,
                favorite = showFavorite && it.favorite,
                poster = remember(it) { it.toPoster() },
                onClick = { onClick(it) },
                onLongClick = { onLongClick(it) },
                onContentClick = { onOptionsClick(it) }
            ) {
                IconButton(
                    onClick = { onOptionsClick(it) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        item {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = "Recommended Content",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if(items.isEmpty())
                        "Based on the most recent favorites"
                    else
                        "Based on the most recent additions to this list",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = 0.78f
                        )
                    ),
                )

            }
        }
        items(recommendations, { "rec${it.contentId}${it.isMovie}" }) {
            ContentListItem(
                title = it.title,
                favorite = showFavorite && it.favorite,
                poster = remember(it) { it.toPoster() },
                onClick = { onClick(it) },
                onLongClick = { onLongClick(it) },
                onContentClick = { onOptionsClick(it) }
            ) {
                IconButton(
                    onClick = { onOptionsClick(it) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddCircleOutline,
                        contentDescription = "add",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        item(key = "recommendation-refresh") {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
            ) {
                Button(
                    onClick = onRefreshClick,
                    enabled = !refreshingRecommendations,
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Text("Refresh")
                }
            }
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
    onContentClick: (() -> Unit)?  = null,
    content: (@Composable () -> Unit)? = null,
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
        endButton = content,
        onEndButtonClick = onContentClick
    )
}
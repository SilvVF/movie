package io.silv.movie.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryComfortableGridItem
import io.silv.core_ui.components.EntryCompactGridItem
import io.silv.core_ui.components.PosterData
import io.silv.core_ui.components.lazy.VerticalGridFastScroller
import io.silv.movie.R
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.browse.movie.components.InLibraryBadge
import io.silv.movie.presentation.toPoster
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun EntryGridItemIconButton(
    onClick: () -> Unit,
    contentDescription: String? = null,
    icon: ImageVector,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp),
        shape = MaterialTheme.shapes.small,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            contentColor = contentColorFor(MaterialTheme.colorScheme.primaryContainer),
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
fun ContentItemSourceCoverOnlyGridItem(
    favorite: Boolean,
    poster: PosterData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    EntryCompactGridItem(
        title = null,
        coverData = poster,
        coverAlpha = if (favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = favorite) },
        onLongClick = { onLongClick() },
        onClick = { onClick() },
        content = content
    )
}

@Composable
fun ContentItemCompactGridItem(
    title: String,
    favorite: Boolean,
    poster: PosterData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    EntryCompactGridItem(
        title = title,
        coverData = poster,
        coverAlpha = if (favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = favorite) },
        onLongClick = { onLongClick() },
        onClick = { onClick() },
        content = content
    )
}

@Composable
fun ContentItemComfortableGridItem(
    title: String,
    favorite: Boolean,
    poster: PosterData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    EntryComfortableGridItem(
        title = title,
        coverData = poster,
        coverAlpha = if (favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = favorite) },
        onLongClick = onLongClick,
        onClick = onClick,
        content = content
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
    onRecommendationClick: (item: ContentItem) -> Unit = {},
    onRecommendationLongClick: (item: ContentItem) -> Unit = {},
    onAddRecommendation: (item: ContentItem) -> Unit = {},
    showFavorite: Boolean = true,
    recommendations: ImmutableList<ContentItem> = persistentListOf(),
    refreshingRecommendations: Boolean = false,
    onRefreshClick: () -> Unit = {}
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
            items(items, { it.itemKey }) {
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
                        ) {
                            EntryGridItemIconButton(
                                onClick = { onOptionsClick(it)},
                                icon = Icons.Default.MoreVert
                            )
                        }
                    }
                    PosterDisplayMode.Grid.CompactGrid -> {
                        ContentItemCompactGridItem(
                            title = it.title,
                            favorite = favorite,
                            poster = poster,
                            onClick = { onClick(it) },
                            onLongClick = { onLongClick(it) },
                        ) {
                            EntryGridItemIconButton(
                                onClick = { onOptionsClick(it)},
                                icon = Icons.Default.MoreVert
                            )
                        }
                    }
                    PosterDisplayMode.Grid.CoverOnlyGrid -> {
                        ContentItemSourceCoverOnlyGridItem(
                            favorite = favorite,
                            poster = poster,
                            onClick = { onClick(it) },
                            onLongClick = { onLongClick(it) },
                        ) {
                            EntryGridItemIconButton(
                                onClick = { onOptionsClick(it)},
                                icon = Icons.Default.MoreVert
                            )
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                Column(
                    modifier = Modifier.padding(22.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.recommended_content_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if(items.isEmpty())
                            stringResource(id = R.string.recommended_content_description_favorites)
                        else
                            stringResource(id = R.string.recommended_content_description),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(
                                alpha = 0.78f
                            )
                        ),
                    )

                }
            }
            items(recommendations, { r -> "recommendation" + r.itemKey }) {
                val poster = remember(it) { it.toPoster() }
                val favorite = showFavorite && it.favorite
                when (mode) {
                    PosterDisplayMode.Grid.ComfortableGrid -> {
                        ContentItemComfortableGridItem(
                            title = it.title,
                            favorite = favorite,
                            poster = poster,
                            onClick = { onRecommendationClick(it) },
                            onLongClick = { onRecommendationLongClick(it) },
                        ) {
                            EntryGridItemIconButton(
                                onClick = { onAddRecommendation(it)},
                                contentDescription = stringResource(id = R.string.add),
                                icon = Icons.Default.AddCircleOutline
                            )
                        }
                    }
                    PosterDisplayMode.Grid.CompactGrid -> {
                        ContentItemCompactGridItem(
                            title = it.title,
                            favorite = favorite,
                            poster = poster,
                            onClick = { onRecommendationClick(it) },
                            onLongClick = { onRecommendationLongClick(it) },
                        ) {
                            EntryGridItemIconButton(
                                onClick = { onAddRecommendation(it)},
                                contentDescription = stringResource(id = R.string.add),
                                icon = Icons.Default.AddCircleOutline
                            )
                        }
                    }
                    PosterDisplayMode.Grid.CoverOnlyGrid -> {
                        ContentItemSourceCoverOnlyGridItem(
                            favorite = favorite,
                            poster = poster,
                            onClick = { onRecommendationClick(it) },
                            onLongClick = { onRecommendationLongClick(it) },
                        ) {
                            EntryGridItemIconButton(
                                onClick = { onAddRecommendation(it)},
                                contentDescription = stringResource(id = R.string.add),
                                icon = Icons.Default.AddCircleOutline
                            )
                        }
                    }
                }
            }
            item(key = "recommendation-refresh", span = { GridItemSpan(maxCurrentLineSpan) }) {
                Button(
                    onClick = onRefreshClick,
                    enabled = !refreshingRecommendations
                ) {
                    Text(stringResource(id = R.string.refresh))
                }
            }
        }
    }
}

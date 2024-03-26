package io.silv.movie.presentation.library.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import coil.request.ImageRequest
import io.silv.core_ui.components.ItemCover
import io.silv.movie.R
import io.silv.movie.data.cache.ListCoverCache
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListItem
import io.silv.movie.presentation.toPoster
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.koinInject

private val ListItemHeight = 72.dp

@Composable
fun ContentListPoster(
    list: ContentList,
    items: ImmutableList<ContentListItem>,
    modifier: Modifier = Modifier
) {
    val cache = koinInject<ListCoverCache>()
    var semaphor by remember { mutableIntStateOf(0) }
    val file = remember(semaphor) { cache.getCustomCoverFile(list.id) }

    val fileExists by remember(semaphor) {
        derivedStateOf { file.exists() }
    }

    LaunchedEffect(list.posterLastModified) {
        semaphor++
    }

    if (fileExists) {
        ContentPreviewDefaults.CustomListPoster(
            modifier = modifier,
            uri = file.toUri()
        )
    } else {
        if (items.size < 4) {
            ContentPreviewDefaults.SingleItemPoster(
                modifier = modifier,
                item = items.first()
            )
        } else {
            ContentPreviewDefaults.MultiItemPosterContentLIst(
                modifier = modifier,
                content = items
            )
        }
    }
}

@Composable
fun ContentListPosterItems(
    list: ContentList,
    items: ImmutableList<ContentItem>,
    modifier: Modifier = Modifier
) {
    val cache = koinInject<ListCoverCache>()
    var semaphor by remember { mutableIntStateOf(0) }
    val file = remember(semaphor) { cache.getCustomCoverFile(list.id) }

    val fileExists by remember(semaphor) {
        derivedStateOf { file.exists() }
    }

    LaunchedEffect(list.posterLastModified) {
        semaphor++
    }

    if (fileExists) {
        ContentPreviewDefaults.CustomListPoster(
            modifier = modifier,
            uri = file.toUri()
        )
    } else {
        if (items.size < 4) {
            ContentPreviewDefaults.SingleItemPoster(
                modifier = modifier,
                item = items.firstOrNull()
            )
        } else {
            ContentPreviewDefaults.MultiItemPoster(
                modifier = modifier,
                items = items
            )
        }
    }
}

object ContentPreviewDefaults {

    @Composable
    fun PlaceholderPoster(
        modifier: Modifier
    ) {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            color = Color.DarkGray
                        )
                    }
                }
        ) {
            Icon(
                imageVector = Icons.Filled.MovieFilter,
                contentDescription = stringResource(id = R.string.filter),
                modifier = Modifier
                    .fillMaxSize(0.5f)
                    .align(Alignment.Center)
            )
        }
    }

    @Composable
    fun MultiItemPosterContentLIst(
        modifier: Modifier,
        content: ImmutableList<ContentListItem>
    ) {
        val contentItems = remember(content) {
            content
                .filterIsInstance<ContentListItem.Item>()
                .take(4)
                .map { it.contentItem }
                .toImmutableList()
        }

        MultiItemPoster(
            modifier = modifier,
            items = contentItems
        )
    }

    @Composable
    fun MultiItemPoster(
        modifier: Modifier,
        items: ImmutableList<ContentItem>,
    ) {
        val trimmed = remember(items) { items.take(4) }

        FlowRow(
            maxItemsInEachRow = 2,
            modifier = modifier.aspectRatio(1f)
        ) {
            trimmed.fastForEach {
                ItemCover.Square(
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    data = remember(it) { it.toPoster() }
                )
            }
        }
    }

    @Composable
    fun LibraryContentPoster(
        modifier: Modifier = Modifier
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary
        val tertiary = MaterialTheme.colorScheme.tertiary

        Box(
            modifier = modifier
                .aspectRatio(1f)
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(primary, secondary, tertiary)
                            )
                        )
                    }
                }
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(0.5f)
                    .align(Alignment.Center)
            )
        }
    }

    @Composable
    fun CustomListPoster(
        modifier: Modifier,
        uri: Uri,
    ) {
        val context = LocalContext.current
        ItemCover.Square(
            modifier = modifier,
            shape = RectangleShape,
            data = ImageRequest.Builder(context)
                .data(uri)
                .build()
        )
    }

    @Composable
    fun SingleItemPoster(
        modifier: Modifier,
        item: ContentItem?
    ) {
        if (item == null) {
            PlaceholderPoster(modifier = modifier)
        } else {
            ItemCover.Square(
                modifier = modifier,
                shape = RectangleShape,
                data = remember(item) { item.toPoster() }
            )
        }
    }

    @Composable
    fun SingleItemPoster(
        modifier: Modifier,
        item: ContentListItem
    ) {
        when (item) {
            is ContentListItem.Item -> {
                SingleItemPoster(
                    modifier = modifier,
                    item = item.contentItem
                )
            }
            is ContentListItem.PlaceHolder -> PlaceholderPoster(modifier = modifier)
        }
    }
}

@Composable
fun LazyGridItemScope.ContentGridPreviewItem(
    cover: @Composable () -> Unit,
    name: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .fillMaxWidth()
        ) {
            cover()
        }
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = name,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                text = if (count == 0)
                    stringResource(id = R.string.content_preview_no_items)
                else
                    stringResource(id = R.string.content_preview_items, count),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.graphicsLayer { alpha = 0.78f }
            )
        }
    }
}

@Composable
fun LazyItemScope.ContentListPreview(
    cover: @Composable () -> Unit,
    name: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.heightIn(0.dp, ListItemHeight),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(0.2f, fill = false)
                .fillMaxHeight()
        ) {
            cover()
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(0.8f, true)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = name,
                maxLines = 2,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                modifier = Modifier.graphicsLayer { alpha = 0.78f }
            )
        }
    }
}

package io.silv.movie.presentation.components.content

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.core_ui.components.CoverPlaceholderColor
import io.silv.core_ui.components.ItemCover
import io.silv.movie.R
import io.silv.movie.core.DiskUtil
import io.silv.movie.data.model.ContentItem
import io.silv.movie.data.model.ContentList
import io.silv.movie.presentation.covers.cache.ListCoverCache
import io.silv.movie.presentation.tabs.SharedElement
import io.silv.movie.presentation.tabs.registerSharedElement
import io.silv.movie.presentation.toPoster
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject

private val ListItemHeight = 72.dp

@Stable
data class ListUri(
    val hash: String,
    val uri: Uri
)

@Suppress("ComposableNaming")
@Composable
fun rememberListUri(list: ContentList): ListUri? {

    val cache= koinInject<ListCoverCache>()
    var semaphor by remember { mutableIntStateOf(0) }

    val fileUri = remember(semaphor) {
        val uri = cache.getCustomCoverFile(list.id)
            .takeIf { it.exists() }
            ?.toUri()
            ?: return@remember null

        ListUri(DiskUtil.hashKeyForDisk(list.id.toString()), uri)
    }

    LaunchedEffect(list.posterLastModified) {
        semaphor++
    }

    return fileUri
}


@Composable
fun ContentListPosterStateFlowItems(
    list: ContentList,
    items: List<StateFlow<ContentItem>>,
    modifier: Modifier = Modifier
) {
    val listUri = rememberListUri(list = list)

    if (listUri != null) {
        ContentPreviewDefaults.CustomListPoster(
            modifier = modifier,
            uri = listUri.uri,
            hash = listUri.hash,
            lastModified = list.posterLastModified
        )
    }else {
        when {
            items.isEmpty() -> ContentPreviewDefaults.PlaceholderPoster(modifier = modifier)
            items.size < 4 -> {

                val item by items[0].collectAsStateWithLifecycle()

                ContentPreviewDefaults.SingleItemPoster(
                    modifier = modifier,
                    item = item
                )
            }
            else -> {
                ContentPreviewDefaults.MultiItemPosterStateFlowItems(
                    modifier = modifier,
                    items = items
                )
            }
        }
    }
}


@Composable
fun ContentListPoster(
    list: ContentList,
    items: List<ContentItem>,
    modifier: Modifier = Modifier
) {
    val listUri = rememberListUri(list = list)

    if (listUri != null) {
        ContentPreviewDefaults.CustomListPoster(
            modifier = modifier,
            uri = listUri.uri,
            hash = listUri.hash,
            lastModified = list.posterLastModified
        )
    }else {
        when {
            items.isEmpty() -> ContentPreviewDefaults.PlaceholderPoster(modifier = modifier)
            items.size < 4 -> ContentPreviewDefaults.SingleItemPoster(
                modifier = modifier,
                item = items.firstOrNull()
            )
            else -> ContentPreviewDefaults.MultiItemPoster(
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
        val bg = MaterialTheme.colorScheme.secondaryContainer
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .drawWithCache {
                    onDrawBehind {
                        drawRect(color = bg)
                    }
                }
        ) {
            Icon(
                imageVector = Icons.Filled.MovieFilter,
                contentDescription = stringResource(id = R.string.filter),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .fillMaxSize(0.5f)
                    .align(Alignment.Center)
            )
        }
    }

    @Composable
    fun MultiItemPosterStateFlowItems(
        modifier: Modifier,
        items: List<StateFlow<ContentItem>>,
    ) {
        val trimmed = remember(items) { items.take(4) }
        Layout(
            content = {
                trimmed.fastForEach {
                    val data by it.collectAsStateWithLifecycle()
                    ItemCover.Square(
                        shape = RectangleShape,
                        data = remember(data) { data.toPoster() }
                    )
                }
            },
            modifier = modifier.aspectRatio(1f)
        ) { measurables, constraints ->

            val placeables = measurables.take(4).map {
                it.measure(
                    constraints.copy(
                        minHeight = 0,
                        minWidth = 0,
                        maxHeight = minOf(constraints.minHeight / 2, constraints.minWidth / 2),
                        maxWidth = minOf(constraints.minHeight / 2, constraints.minWidth / 2)
                    )
                )
            }

            layout(constraints.minWidth, constraints.minHeight) {
                placeables.forEachIndexed { i, item ->
                    when (i) {
                        0 -> item.place(0, 0)
                        1 -> item.place(constraints.maxWidth / 2,  0)
                        2 -> item.place(0, constraints.maxHeight / 2)
                        3 -> item.place(constraints.maxWidth / 2, constraints.maxHeight / 2)
                    }
                }
            }
        }
    }

    @Composable
    fun MultiItemPoster(
        modifier: Modifier,
        items: List<ContentItem>,
    ) {
        val trimmed = remember(items) { items.take(4) }

        Layout(
            content = {
                trimmed.fastForEach {
                    ItemCover.Square(
                        shape = RectangleShape,
                        data = remember(it) { it.toPoster() }
                    )
                }
            },
            modifier = modifier.aspectRatio(1f)
        ) { measurables, constraints ->

            val placeables = measurables.take(4).map {
                it.measure(
                    constraints.copy(
                        minHeight = 0,
                        minWidth = 0,
                        maxHeight = constraints.maxHeight / 2,
                        maxWidth = constraints.maxWidth / 2
                    )
                )
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEachIndexed { i, item ->
                    when (i) {
                        0 -> item.place(0, 0)
                        1 -> item.place(constraints.maxWidth / 2,  0)
                        2 -> item.place(0, constraints.maxHeight / 2)
                        3 -> item.place(constraints.maxWidth / 2, constraints.maxHeight / 2)
                    }
                }
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
                                colors = buildList {
                                    if (primary == secondary)
                                        add(primary)
                                    else
                                        addAll(listOf(primary, secondary))
                                    add(tertiary)
                                },
                            )
                        )
                    }
                }
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
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
        hash: String = "",
        lastModified: Long = 0L,
    ) {
        val context = LocalContext.current
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(true)
                .diskCacheKey("$hash,$lastModified")
                .memoryCacheKey("$hash,$lastModified")
                .build(),
            placeholder = remember { ColorPainter(CoverPlaceholderColor) },
            contentDescription = null,
            modifier = modifier
                .aspectRatio(1f)
                .clip(RectangleShape),
            contentScale = ContentScale.Crop,
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
                data = remember(item.posterLastUpdated) { item.toPoster() },
            )
        }
    }
}

@Composable
fun LazyGridItemScope.ContentGridPreviewItem(
    cover: @Composable () -> Unit,
    listId: Long,
    name: String,
    count: Int,
    modifier: Modifier = Modifier,
    pinned: Boolean = false
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (pinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(45f)
                            .padding(end = 2.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = name,
                    modifier = Modifier.registerSharedElement(SharedElement.From(SharedElement.PREFIX_LIST_NAME + name)),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                text = if (count == 0)
                    stringResource(id = R.string.content_preview_no_items)
                else
                    stringResource(id = R.string.content_preview_items, count),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 0.78f }
            )
        }
    }
}

@Composable
fun LazyItemScope.ContentListPreview(
    cover: @Composable () -> Unit,
    name: String,
    description: String,
    pinned: Boolean = false,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier
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
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = name,
                    modifier = textModifier,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (pinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(45f)
                                .padding(end = 2.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer { alpha = 0.78f }
                    )
                }
            }
        }
}

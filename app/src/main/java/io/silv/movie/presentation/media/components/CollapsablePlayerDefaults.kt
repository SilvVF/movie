package io.silv.movie.presentation.media.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Brands
import compose.icons.fontawesomeicons.brands.Youtube
import io.silv.core_ui.components.AnimatedEqualizer
import io.silv.core_ui.components.DotSeparatorText
import io.silv.core_ui.components.TooltipIconButton
import io.silv.core_ui.util.conditional
import io.silv.core_ui.util.playOnYoutube
import io.silv.movie.R
import io.silv.movie.data.model.Trailer
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object CollapsablePlayerDefaults {

    @Composable
    fun VideoQueueItem(
        reorderableState: ReorderableLazyListState,
        onMute: () -> Unit,
        trailer: Trailer,
        idx: Int,
    ) {
        RerorderableVideoQueueItem(reorderableState, onMute, trailer, idx)
    }

    @Composable
    fun VideoDescription(
        trailer: Trailer,
        modifier: Modifier = Modifier
    ) {
        Surface(shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)) {
            PlayerMediaItemInfo(
                item = trailer,
                modifier = modifier
            )
        }
    }

    @Composable
    fun RowScope.Actions(
        currentTrailer: Trailer?,
        playing: Boolean,
        onPlayClick: () -> Unit,
        onPauseClick: () -> Unit,
        onCloseClick: () -> Unit
    ) {
        CollapsedPlayerTitleAndActions(
            currentTrailer = currentTrailer,
            playing = playing,
            onPlayClick = onPlayClick,
            onPauseClick = onPauseClick,
            onCloseClick = onCloseClick
        )
    }
}

@Composable
private fun RerorderableVideoQueueItem(
    reorderableState: ReorderableLazyListState,
    mutePlayer: () -> Unit,
    trailer: Trailer,
    idx: Int,
) {
    ReorderableItem(
        state = reorderableState,
        key =  { trailer },
        index = idx,
    ) {dragging ->
        val elevation by animateDpAsState(
            label = "drag-elevation",
            targetValue = if (dragging) 8.dp else 0.dp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.wrapContentSize(),
        ) {
            if (idx == 0) {
                AnimatedEqualizer(
                    onClick = mutePlayer
                )
            } else {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(2.dp)
                        .detectReorder(reorderableState),
                    tint = MaterialTheme.colorScheme.surfaceTint
                )
            }
            val cardShape = CardDefaults.elevatedShape
            VideoMediaItem(
                onThumbnailClick = {  },
                item = trailer,
                thumbnailProvider = {
                    if (trailer.site == "YouTube") {
                        "https://img.youtube.com/vi/${trailer.key}/0.jpg"
                    } else {
                        ""
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        shadowElevation = elevation.toPx()
                        shape = cardShape
                    }
                    .conditional(idx != 1) {
                        detectReorderAfterLongPress(reorderableState)
                    },
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)
                )
            )
        }
    }
}

@Composable
private fun PlayerMediaItemInfo(
    item: Trailer,
    modifier: Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                item.name,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            Row(
                modifier = Modifier.alpha(0.78f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                val formattedDateTime = remember {
                    runCatching {

                        val dateTime = ZonedDateTime.parse(item.publishedAt)

                        val formatter =
                            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())
                        dateTime.format(formatter)
                    }
                        .getOrDefault("")
                }
                Text(
                    text = item.type,
                    style = MaterialTheme.typography.labelSmall
                )
                DotSeparatorText()
                Text(
                    text = formattedDateTime,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (item.official) {
                val context = LocalContext.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.wrapContentSize()
                ) {
                    if (item.site == "YouTube") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.alpha(0.78f),
                        ) {
                            TooltipIconButton(
                                onClick = { /*TODO*/ },
                                tooltip = stringResource(id = R.string.view_on_youtube),
                                imageVector = FontAwesomeIcons.Brands.Youtube,
                                modifier = Modifier.size(22.dp)
                            )
                            DotSeparatorText(modifier = Modifier.align(Alignment.CenterVertically))
                            Text(stringResource(id = R.string.youtube), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text(
                        stringResource(id = R.string.verified),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.alpha(0.78f)
                    )
                    TooltipIconButton(
                        onClick = {
                            context.playOnYoutube(item.key)
                        },
                        tooltip = stringResource(id = R.string.verified),
                        imageVector = Icons.Filled.CheckCircle,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.CollapsedPlayerTitleAndActions(
    currentTrailer: Trailer?,
    playing: Boolean,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Text(
        text = currentTrailer?.name ?: "",
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    AnimatedContent(
        targetState = playing,
        label = "",
        modifier = Modifier.wrapContentSize()
    ) { isPlaying ->
        if (!isPlaying) {
            IconButton(
                onClick = onPlayClick
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(id = R.string.play)
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedEqualizer(
                    onClick = { /*TODO*/ }
                )
                IconButton(
                    onClick = onPauseClick
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = stringResource(id = R.string.pause)
                    )
                }
            }
        }
    }
    IconButton(
        onClick = onCloseClick
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(id = R.string.close)
        )
    }
}

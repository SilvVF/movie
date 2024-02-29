package io.silv.movie.presentation.media

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.silv.core_ui.components.AnimatedEqualizer
import io.silv.core_ui.util.conditional
import io.silv.movie.data.trailers.Trailer
import io.silv.movie.presentation.movie.view.components.VideoMediaItem
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.detectReorderAfterLongPress

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

    context(RowScope)
    @Composable
    fun Actions(
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
            if (idx != 0) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(2.dp)
                        .detectReorder(reorderableState),
                    tint = MaterialTheme.colorScheme.surfaceTint
                )
            } else {
                AnimatedEqualizer(
                    onClick = mutePlayer
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
                    .conditional(idx != 0) {
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
                    contentDescription = null
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
                        contentDescription = null
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
            contentDescription = null
        )
    }
}

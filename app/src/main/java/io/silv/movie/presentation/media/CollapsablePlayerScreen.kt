package io.silv.movie.presentation.media

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.silv.core_ui.components.AnimatedEqualizer
import io.silv.core_ui.util.conditional
import io.silv.movie.data.trailers.Trailer
import io.silv.movie.presentation.movie.view.components.VideoMediaItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import timber.log.Timber

@Composable
fun BoxScope.CollapsablePlayerScreen(
    collapsableVideoState: CollapsableVideoState,
    initial: Long,
    videos: ImmutableList<Trailer>,
    onDismissRequested: () -> Unit
) {

    val playerState  = rememberPlayerState()
    var data by remember(videos) { mutableStateOf(videos) }

    LaunchedEffect(initial) {
        playerState.initialized()
            .filter { it }
            .collectLatest {

                val trailer = data.first { it.id == initial }

                if (playerState.currentVideoId != trailer.key) {
                    playerState.loadVideo(trailer.key, 0f)
                }
        }
    }

    val playing by remember {
        derivedStateOf {
            data.firstOrNull { it.key == playerState.currentVideoId } ?: data.first { it.id == initial }
        }
    }

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            Timber.d("$from, $to")
            if (to.index < data.size) {
                data = data.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
                    .toImmutableList()
            }
        },
        onDragEnd = { from, to ->
           if (to == 0) {
              playerState.loadVideo(data[from].key, 0f)
           }
        },
        listState = rememberLazyListState(),
    )

    CollapsableVideoLayout(
        actions = {
            CollapsedPlayerTitleAndActions(
                playing = playing,
                playerState = playerState,
                collapsableVideoState = collapsableVideoState
            )
        },
        collapsableVideoState = collapsableVideoState,
        modifier = Modifier,
        player = {
            YoutubeVideoPlayer(Modifier, playerState, collapsableVideoState.state.currentValue == CollapsableVideoAnchors.Start)
        },
        content = {
            itemsIndexed(data, { _, it -> it.id }) {idx, trailer ->
                ReorderableItem(
                    reorderableState = reorderState,
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
                                    .detectReorder(reorderState),
                                tint = MaterialTheme.colorScheme.surfaceTint
                            )
                        } else {
                            AnimatedEqualizer(
                                onClick = { /*TODO*/ }
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
                                    detectReorderAfterLongPress(reorderState)
                                },
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)
                            )
                        )
                    }
                }
            }
        },
        reorderState = reorderState,
        onDismissRequested = onDismissRequested
    )
}

@Composable
fun RowScope.CollapsedPlayerTitleAndActions(
    playing: Trailer,
    playerState: PlayerState,
    collapsableVideoState: CollapsableVideoState
) {
    val scope = rememberCoroutineScope()
    Text(
        text = playing.name,
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
    AnimatedContent(
        targetState = playerState.playing,
        label = "",
        modifier = Modifier.wrapContentSize()
    ) { playing ->
        if (!playing) {
            IconButton(
                onClick = { playerState.play() }
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
                    onClick = { playerState.pause() }
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
        onClick = {
            scope.launch {
                collapsableVideoState.state.animateTo(CollapsableVideoAnchors.Dismiss)
            }
        }
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null
        )
    }
}
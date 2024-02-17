package io.silv.movie.presentation

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.skydoves.orbital.Orbital
import com.skydoves.orbital.animateTransformation
import com.skydoves.orbital.rememberContentWithOrbitalScope
import io.silv.data.movie.interactor.MovieVideo
import io.silv.movie.DragAnchors
import io.silv.movie.presentation.media.YoutubeVideoPlayer
import io.silv.movie.presentation.movie.view.components.VideoMediaItem
import kotlinx.collections.immutable.ImmutableList


@Composable
fun rememberAnchoredDragState(
    minHeight: Dp = 90.dp,
    offset: (progress: Float) -> Dp,
    maxHeight: Dp
): MediaQueueDragState {

    val density = LocalDensity.current

    val dragAnchorEnd = (maxHeight - minHeight).value

    val state = remember(dragAnchorEnd) {
        AnchoredDraggableState(
            initialValue = DragAnchors.Start,
            positionalThreshold = { distance: Float -> distance * 0.2f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            animationSpec = tween(),
        ).apply {
            updateAnchors(
                DraggableAnchors {
                    DragAnchors.Start at 0f
                    DragAnchors.End at dragAnchorEnd
                }
            )
        }
    }

    return remember {
        MediaQueueDragState(state, maxHeight, dragAnchorEnd, offset)
    }
}

class MediaQueueDragState(
    val draggableState: AnchoredDraggableState<DragAnchors>,
    maxHeight: Dp,
    dragAnchorEnd: Float,
    offset: (progress: Float) -> Dp,
) {

    val progress by derivedStateOf {
        (1f - (draggableState.offset / dragAnchorEnd))
            .coerceIn(0f..1f)
            .takeIf { !it.isNaN() } ?: 1f

    }

    val isTransformed by derivedStateOf { progress > 0.3f }

    val collapsedAlpha by derivedStateOf { lerp(1f, 0f, progress / 0.2f) }

    val expandedAlpha by derivedStateOf { lerp(0f, 1f, progress / 0.8f) }

    val height by derivedStateOf {
        maxHeight - draggableState.requireOffset().dp + offset(progress)
    }
}

@Composable
fun MediaQueueAnchoredDraggable(
    videos: ImmutableList<MovieVideo>,
    state: MediaQueueDragState,
    modifier: Modifier = Modifier
) {

    val poster = rememberContentWithOrbitalScope {
        YoutubeVideoPlayer(
            modifier = if (state.isTransformed) {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .zIndex(2f)
            } else {
                Modifier
                    .fillMaxWidth(
                        lerp(0.4f, 1f, state.progress / 0.2f)
                    )
                    .aspectRatio(16f / 9f)
                    .zIndex(2f)
            }
                .animateTransformation(this, spring()),
            videoId = videos.first().key,
        )
    }

    val scrollState = rememberLazyListState()

    Surface(
        modifier
            .clipToBounds()
            .fillMaxWidth()
            .height(state.height)
            .anchoredDraggable(state.draggableState, Orientation.Vertical)
    ) {
        Orbital(
            modifier = Modifier
        ) {
            if (state.isTransformed) {
                Column {
                    poster()
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .alpha(state.expandedAlpha)
                    ) {
                        items(videos) {
                            VideoMediaItem(
                                onThumbnailClick = { /*TODO*/ },
                                item = it,
                                thumbnailProvider = {
                                    if (it.site == "YouTube") {
                                        "https://img.youtube.com/vi/${it.key}/0.jpg"
                                    } else {
                                        ""
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    poster()
                    Text(
                        text = videos.first().name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .zIndex(1f)
                            .alpha(state.collapsedAlpha)
                    )
                    IconButton(
                        onClick = { /*TODO*/ },
                        modifier = Modifier.alpha(state.collapsedAlpha)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                    }
                    IconButton(
                        onClick = { /*TODO*/ },
                        modifier = Modifier.alpha(state.collapsedAlpha)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}
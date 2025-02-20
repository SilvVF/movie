package io.silv.movie.presentation.media.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import io.silv.movie.R
import io.silv.movie.presentation.media.PlayerPresenter
import io.silv.movie.presentation.media.PresenterState
import io.silv.movie.presentation.media.StreamState
import io.silv.movie.presentation.media.components.CollapsablePlayerDefaults.Actions
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.rememberReorderableLazyListState


@Composable
fun CollapsablePlayerScreen(
    videoState: VideoState,
    state: PresenterState,
    reorderState: ReorderableLazyListState,
    modifier: Modifier = Modifier
) {
    DefaultSizeCollapsableVideoLayout(
        modifier = modifier,
        reorderState = reorderState,
        actions = {
            Actions(
                currentTrailer = state.queue.firstOrNull(),
                playing =  state.playing,
                onPlayClick = {
                    videoState.sendPlayerEvent(PlayerPresenter.PlayerEvent.Play)
                },
                onCloseClick = { videoState.dismiss() },
                onPauseClick = {
                    videoState.sendPlayerEvent(PlayerPresenter.PlayerEvent.Pause)
                }
            )
        },
        videoState = videoState,
        player = {
            when(val ss = state.streamState) {
                is StreamState.Failure -> {
                    Box(
                        modifier = Modifier
                            .aspectRatio(16f / 9f)
                            .fillMaxWidth()
                    ) {
                        Text("Error Loading Video ${ss.message}", Modifier.align(Alignment.Center))
                    }
                }
                is StreamState.Success -> {
                    AndroidView(
                        modifier = Modifier
                            .aspectRatio(16f / 9f)
                            .fillMaxWidth(),
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = videoState.player
                            }
                        }
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .aspectRatio(16f / 9f)
                            .fillMaxWidth()
                    ) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                }
            }
        },
        pinnedContent = {
            state.queue.firstOrNull()?.let { trailer ->
                CollapsablePlayerDefaults.VideoDescription(
                    trailer = trailer,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                )
            }
        },
        scrollToTopButton = { scrollToTop ->
            FilledIconButton(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(
                        elevation = 6.0.dp,
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .graphicsLayer { alpha = 0.92f },
                onClick = scrollToTop,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = stringResource(id = R.string.scroll_to_top)
                )
            }
        }
    ) {
        itemsIndexed(
            items = state.queue,
            key = { _, it -> it.id }
        ) {idx, trailer ->
            CollapsablePlayerDefaults.VideoQueueItem(
                reorderableState = reorderState,
                trailer = trailer,
                idx = idx,
                onMute = {
                    videoState.sendPlayerEvent(PlayerPresenter.PlayerEvent.Mute)
                }
            )
        }
    }
}



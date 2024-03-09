package io.silv.movie.presentation.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.silv.core_ui.components.Scroller
import io.silv.movie.PlayerViewModel
import org.burnoutcrew.reorderable.ItemPosition
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.rememberReorderableLazyListState


@Composable
fun CollapsablePlayerScreen(
    collapsableVideoState: CollapsableVideoState,
    onDismissRequested: () -> Unit,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val reorderState = rememberReorderableLazyListState(
        onMove = playerViewModel::onMove,
        canDragOver = { draggedOver: ItemPosition, dragging: ItemPosition ->
            draggedOver.index != 0 || dragging.index != 0
        }
    )

    DefaultSizeCollapsableVideoLayout(
        modifier = modifier,
        reorderState = reorderState,
        onDismissRequested = onDismissRequested,
        actions = {
            CollapsablePlayerDefaults.Actions(
                currentTrailer = playerViewModel.currentTrailer,
                playing = playerViewModel.playing,
                onPlayClick = {
                    playerViewModel.sendPlayerEvent(PlayerViewModel.PlayerEvent.Play)
                },
                onCloseClick = { collapsableVideoState.dismiss() },
                onPauseClick = {
                    playerViewModel.sendPlayerEvent(PlayerViewModel.PlayerEvent.Pause)
                }
            )
        },
        collapsableVideoState = collapsableVideoState,
        player = {
            playerViewModel.streams?.let {
                PipedApiPlayer(
                    playerViewModel,
                    modifier = Modifier
                        .aspectRatio(16f / 9f)
                        .fillMaxWidth()
                )
            } ?: Box(
                modifier = Modifier
                    .aspectRatio(16f / 9f)
                    .fillMaxWidth()
            ) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        },
    ) {
        stickyHeader(
            key = Scroller.STICKY_HEADER_KEY_PREFIX
        ) {
            playerViewModel.currentTrailer?.let { trailer ->
                ReorderableItem(
                    reorderableState = reorderState,
                    key = "sticky-header",
                    index = 0
                ) {
                    CollapsablePlayerDefaults.VideoDescription(
                        trailer = trailer,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
        itemsIndexed(
            items = playerViewModel.trailerQueue,
            key = { _, it -> it.id }
        ) {idx, trailer ->
            CollapsablePlayerDefaults.VideoQueueItem(
                reorderableState = reorderState,
                trailer = trailer,
                idx = idx + 1,
                onMute = {
                    playerViewModel.sendPlayerEvent(PlayerViewModel.PlayerEvent.Mute)
                }
            )
        }
    }
}



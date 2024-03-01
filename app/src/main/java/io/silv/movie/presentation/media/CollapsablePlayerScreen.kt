package io.silv.movie.presentation.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.silv.movie.MainViewModel
import org.burnoutcrew.reorderable.rememberReorderableLazyListState


@Composable
fun CollapsablePlayerScreen(
    collapsableVideoState: CollapsableVideoState,
    onDismissRequested: () -> Unit,
    playerViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {


    val reorderState = rememberReorderableLazyListState(
        onMove = playerViewModel::onMove,
    )

    CollapsableVideoLayout(
        modifier = modifier,
        reorderState = reorderState,
        onDismissRequested = onDismissRequested,
        actions = {
            CollapsablePlayerDefaults.Actions(
                currentTrailer = playerViewModel.currentTrailer,
                playing = playerViewModel.playing,
                onPlayClick = {
                    playerViewModel.sendPlayerEvent(MainViewModel.PlayerEvent.Play)
                },
                onCloseClick = { collapsableVideoState.dismiss() },
                onPauseClick = {
                    playerViewModel.sendPlayerEvent(MainViewModel.PlayerEvent.Pause)
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
        itemsIndexed(
            items = playerViewModel.trailerQueue,
            key = { _, it -> it.id }
        ) {idx, trailer ->
            CollapsablePlayerDefaults.VideoQueueItem(
                reorderableState = reorderState,
                trailer = trailer,
                idx = idx,
                onMute = {
                    playerViewModel.sendPlayerEvent(MainViewModel.PlayerEvent.Mute)
                }
            )
        }
    }
}



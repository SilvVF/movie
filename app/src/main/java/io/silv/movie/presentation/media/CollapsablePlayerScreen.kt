package io.silv.movie.presentation.media

import WebViewScreenContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.silv.movie.PlayerViewModel
import io.silv.movie.R
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.trailers.Trailer
import org.burnoutcrew.reorderable.rememberReorderableLazyListState


@Composable
fun CollapsablePlayerScreenContent(
    collapsableVideoState: CollapsableVideoState,
    onDismissRequested: () -> Unit,
    contentItem: ContentItem,
    modifier: Modifier = Modifier
) {
    val reorderState = rememberReorderableLazyListState(
        onMove = { _, _ ->},
    )

    val trailer = remember(contentItem) {
        Trailer(
            contentItem.contentId.toString(),
            contentItem.contentId.toString(),
            contentItem.title,
            true,
            publishedAt = "",
            site = "",
            size = 1080,
            type = "full video"
        )
    }

    DefaultSizeCollapsableVideoLayout(
        modifier = modifier,
        reorderState = reorderState,
        onDismissRequested = onDismissRequested,
        actions = {
            CollapsablePlayerDefaults.Actions(
                currentTrailer =  trailer,
                playing = true,
                onPlayClick = {},
                onCloseClick = { collapsableVideoState.dismiss() },
                onPauseClick = {}
            )
        },
        collapsableVideoState = collapsableVideoState,
        player = {
            val url = remember(contentItem) {
                val movieBaseUrl = "https://vidsrc.to/embed/movie/"
                val showBaseUrl = "https://vidsrc.to/embed/tv/"

              if(contentItem.isMovie) {
                    "$movieBaseUrl${contentItem.contentId}"
                } else {
                    "$showBaseUrl${contentItem.contentId}"
                }
            }
            WebViewScreenContent(
                url = url,
                modifier = Modifier.fillMaxSize()
            )
        },
        pinnedContent = {
            CollapsablePlayerDefaults.VideoDescription(
                trailer = trailer,
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            )
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
    ) {}
}

@Composable
fun CollapsablePlayerScreen(
    collapsableVideoState: CollapsableVideoState,
    onDismissRequested: () -> Unit,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val reorderState = rememberReorderableLazyListState(
        onMove = playerViewModel::onMove,
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
        pinnedContent = {
            playerViewModel.currentTrailer?.let {
                CollapsablePlayerDefaults.VideoDescription(
                    trailer = it,
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
            items = playerViewModel.trailerQueue,
            key = { _, it -> it.id }
        ) {idx, trailer ->
            CollapsablePlayerDefaults.VideoQueueItem(
                reorderableState = reorderState,
                trailer = trailer,
                idx = idx,
                onMute = {
                    playerViewModel.sendPlayerEvent(PlayerViewModel.PlayerEvent.Mute)
                }
            )
        }
    }
}



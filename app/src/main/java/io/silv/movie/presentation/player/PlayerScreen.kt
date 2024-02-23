package io.silv.movie.presentation.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import io.silv.data.trailers.Trailer
import io.silv.movie.CollapsableVideoLayout
import io.silv.movie.CollapsableVideoState
import io.silv.movie.presentation.media.YoutubeVideoPlayer
import io.silv.movie.presentation.media.rememberPlayerState
import io.silv.movie.presentation.movie.view.components.VideoMediaItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.collectLatest

@Composable
fun BoxScope.CollapsablePlayerScreen(
    state: CollapsableVideoState,
    playing: Trailer,
    videos: ImmutableList<Trailer>,
    onDismissRequested: () -> Unit
) {

    val playerState  = rememberPlayerState()

    LaunchedEffect(playing) {
        playerState.initialized().collectLatest {

            if (!it)
                return@collectLatest

            if (playerState.currentVideoId != playing.key) {

                playerState.queueVideo(playing.key, 0f)
            }
        }
    }

    CollapsableVideoLayout(
        actions = {
            Text(
                text = playing.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

            IconButton(
                onClick = onDismissRequested
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null
                )
            }
        },
        collapsableVideoState = state,
        modifier = Modifier,
        player = {
            YoutubeVideoPlayer(Modifier, playerState)
        },
        content = {
            items(videos) {
                VideoMediaItem(onThumbnailClick = { /*TODO*/ }, item = it) {
                    if (it.site == "YouTube") {
                        "https://img.youtube.com/vi/${it.key}/0.jpg"
                    } else {
                        ""
                    }
                }
            }
        },
        onDismissRequested = onDismissRequested
    )
}
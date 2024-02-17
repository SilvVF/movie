package io.silv.movie.presentation.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import cafe.adriel.voyager.core.screen.Screen
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView


data class PlayerScreen(
    val videoId: String
): Screen {

    @Composable
    override fun Content() {

    }
}

@Composable
fun YoutubeVideoPlayer(
    modifier: Modifier,
    videoId: String,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current

    var currentSecond by rememberSaveable { mutableFloatStateOf(0f) }

    val youtubePlayerListener = remember {
        object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.loadVideo(videoId, currentSecond)
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                super.onCurrentSecond(youTubePlayer, second)
                currentSecond = second
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            YouTubePlayerView(it).apply {
                lifecycle.lifecycle.addObserver(this)
                addYouTubePlayerListener(youtubePlayerListener)
            }
        }
    )
}
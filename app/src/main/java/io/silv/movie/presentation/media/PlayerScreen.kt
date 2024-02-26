package io.silv.movie.presentation.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import io.silv.movie.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


@Composable
fun rememberPlayerState(
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
): PlayerState = rememberSaveable(
    saver = Saver(
        save = {
            arrayOf(it.currentVideoId, it.currentSecond)
        },
        restore  = {
            PlayerState(
                initialVideoId = it[0] as? String,
                initialSecond = it[1] as? Float,
                lifecycle
            )
        }
    )
) {
    PlayerState(null, null, lifecycle)
}

@Stable
class PlayerState(
    private val initialVideoId: String?,
    private val initialSecond: Float?,
    private val lifecycle: Lifecycle
) {

    var currentVideoId by mutableStateOf(initialVideoId)
        private set

    var currentSecond by mutableFloatStateOf(0f)
        private set

    private var player: YouTubePlayer? = null

    fun initialized(): Flow<Boolean> {
        return flow {
            while(player == null) {
                emit(false)
                delay(10)
            }
            emit(true)
        }
    }


    var playbackState by mutableStateOf(PlayerConstants.PlayerState.UNKNOWN)

    val playing by derivedStateOf {
        playbackState == PlayerConstants.PlayerState.PLAYING
    }

    val youtubePlayerListener =
        object : AbstractYouTubePlayerListener() {
            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                super.onCurrentSecond(youTubePlayer, second)
                currentSecond = second
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                super.onStateChange(youTubePlayer, state)
                playbackState = state
            }
        }

    fun init(p: YouTubePlayer) {
        player = p
        loadVideo(
            initialVideoId ?: return,
            initialSecond ?: return
        )
    }


    fun pause() {
        player?.pause()
    }

    fun loadVideo(videoId: String, second: Float = 0f){
        player?.loadVideo(videoId, second)
    }

    fun play() {
        player?.play()
    }

    fun onDispose() {
        player?.removeListener(youtubePlayerListener)
        player = null
    }
}

@Composable
fun YoutubeVideoPlayer(
    modifier: Modifier,
    playerState: PlayerState,
    expanded: Boolean,
) {
    val lifecycle = LocalLifecycleOwner.current

    val playerOptions = remember(expanded) {
        IFramePlayerOptions.Builder()
            .modestBranding(1)
            .autoplay(1)
            .rel(0)
            .ivLoadPolicy(3)
            .ccLoadPolicy(0)
            .controls(if (expanded) 1 else 0)
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = {
            YouTubePlayerView(it).apply {
                inflateCustomPlayerUi(R.layout.custom_player_ui)
                enableAutomaticInitialization = false
                lifecycle.lifecycle.addObserver(this)
                initialize(
                    playerState.youtubePlayerListener,
                    true,
                    playerOptions
                )
                getYouTubePlayerWhenReady(
                    object : YouTubePlayerCallback {
                        override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                            playerState.init(youTubePlayer)
                        }
                    }
                )
            }
        }
    )
}
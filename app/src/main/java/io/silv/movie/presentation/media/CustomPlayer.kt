@file:OptIn(UnstableApi::class)

package io.silv.movie.presentation.media

import android.content.Context
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.chromium.net.CronetEngine
import org.koin.compose.getKoin
import java.util.concurrent.Executors


private inline fun DefaultTrackSelector.updateParameters(
    actions: DefaultTrackSelector.Parameters.Builder.() -> Unit
) = setParameters(buildUponParameters().apply(actions))


private fun createExoPlayer(
    context: Context,
    playerListener: Player.Listener
): ExoPlayer {
    // control for the track sources like subtitles and audio source
    val trackSelector = DefaultTrackSelector(context)

    val exoPlayer = PlayerHelper.createPlayer(context, trackSelector, false)
    exoPlayer.setWakeMode(C.WAKE_MODE_NETWORK)
    exoPlayer.addListener(playerListener)
    return exoPlayer
}


@Composable
fun PipedApiPlayer(
    playerViewModel: PipedApiViewModel,
    modifier: Modifier,
) {

    val context = LocalContext.current
    val koin = getKoin()

    val cronetDataSourceFactory = remember {
        CronetDataSource.Factory(
            koin.get<CronetEngine>(),
            Executors.newCachedThreadPool()
        )
    }

    val playerListener = remember {
        object : Player.Listener {

        }
    }

    val exoPlayer= remember {
        createExoPlayer(context, playerListener)
    }


    DisposableEffect(playerViewModel) {

        playerViewModel.player = exoPlayer

        onDispose {
            exoPlayer.pause()
            playerViewModel.player = null
        }
    }


    LaunchedEffect(exoPlayer, playerViewModel.streams) {

        val streams = playerViewModel.streams ?: return@LaunchedEffect

         when {
            streams.hls != null -> {
                val hlsMediaSourceFactory = HlsMediaSource.Factory(cronetDataSourceFactory)
                    .setPlaylistParserFactory(YoutubeHlsPlaylistParser.Factory())

                val mediaSource = hlsMediaSourceFactory.createMediaSource(
                    playerViewModel.createMediaItem(
                        DashHelper.unwrapUrl(streams.hls).toUri(),
                        MimeTypes.APPLICATION_M3U8
                    )
                )
                withContext(Dispatchers.Main) { exoPlayer.setMediaSource(mediaSource) }
            }
            // NO STREAM FOUND
            else -> {
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
        exoPlayer.seekTo(0)
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
            }
        }
    )
}


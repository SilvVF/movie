package io.silv.movie.presentation.media.util

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import okhttp3.OkHttpClient


object PlayerHelper {

    const val ROLE_FLAG_AUTO_GEN_SUBTITLE = C.ROLE_FLAG_SUPPLEMENTARY
    private const val MINIMUM_BUFFER_DURATION = 1000 * 10 // exo default is 50s


    /**
     * Create a basic player, that is used for all types of playback situations inside the app
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun createPlayer(
        context: Context,
        trackSelector: DefaultTrackSelector,
        isBackgroundMode: Boolean
    ): ExoPlayer {

        val cronetDataSourceFactory = OkHttpDataSource.Factory(
            OkHttpClient.Builder().build()
        )
        val dataSourceFactory = DefaultDataSource.Factory(context, cronetDataSourceFactory)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(context)
            .setUsePlatformDiagnostics(false)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(getLoadControl())
            .setAudioAttributes(audioAttributes, true)
            .setUsePlatformDiagnostics(false)
            .build()
            .apply {
                loadPlaybackParams(isBackgroundMode)
            }
    }

    /**
     * Get the load controls for the player (buffering, etc)
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            // cache the last three minutes
            .setBackBuffer(1000 * 60 * 3, true)
            .setBufferDurationsMs(
                MINIMUM_BUFFER_DURATION,
                50000,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()
    }

    /**
     * Load playback parameters such as speed and skip silence
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun ExoPlayer.loadPlaybackParams(isBackgroundMode: Boolean = false): ExoPlayer {
        skipSilenceEnabled = false
        val speed = if (isBackgroundMode) 1f else 1f
        playbackParameters = PlaybackParameters(speed, 1.0f)
        return this
    }
}

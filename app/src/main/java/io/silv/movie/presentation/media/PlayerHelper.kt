package io.silv.movie.presentation.media

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import io.silv.movie.network.model.Streams
import org.chromium.net.CronetEngine
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.concurrent.Executors


object PlayerHelper: KoinComponent {
    private const val ACTION_MEDIA_CONTROL = "media_control"
    const val CONTROL_TYPE = "control_type"
    const val SPONSOR_HIGHLIGHT_CATEGORY = "poi_highlight"
    const val ROLE_FLAG_AUTO_GEN_SUBTITLE = C.ROLE_FLAG_SUPPLEMENTARY
    private const val MINIMUM_BUFFER_DURATION = 1000 * 10 // exo default is 50s

    /**
     * The maximum amount of time to wait until the video starts playing: 10 minutes
     */
    const val MAX_BUFFER_DELAY = 10 * 60 * 1000L

    val repeatModes = listOf(
        Player.REPEAT_MODE_OFF to "off",
        Player.REPEAT_MODE_ONE to "one",
        Player.REPEAT_MODE_ALL to "all"
    )

    /**
     * A list of all categories that are not disabled by default
     * Also update `sponsorblock_settings.xml` when modifying this!
     */
    private val sbDefaultValues = mapOf(
        "sponsor" to SbSkipOptions.AUTOMATIC,
        "selfpromo" to SbSkipOptions.AUTOMATIC
    )

    /**
     * Create a base64 encoded DASH stream manifest
     */
    fun createDashSource(streams: Streams, context: Context, disableProxy: Boolean): Uri {
        val manifest = DashHelper.createManifest(
            streams,
            context.supportsHdr(),
            disableProxy
        )

        // encode to base64
        val encoded = Base64.encodeToString(manifest.toByteArray(), Base64.DEFAULT)
        return "data:application/dash+xml;charset=utf-8;base64,$encoded".toUri()
    }

    /**
     * Create a basic player, that is used for all types of playback situations inside the app
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun createPlayer(
        context: Context,
        trackSelector: DefaultTrackSelector,
        isBackgroundMode: Boolean
    ): ExoPlayer {

        val cronetDataSourceFactory = CronetDataSource.Factory(
            get<CronetEngine>(),
            Executors.newCachedThreadPool()
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

    enum class SbSkipOptions {
        OFF,
        VISIBLE,
        MANUAL,
        AUTOMATIC,
        AUTOMATIC_ONCE
    }

    private fun Context.supportsHdr(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val display = ContextCompat.getDisplayOrDefault(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                display.isHdr
            } else {
                @Suppress("DEPRECATION")
                display.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() ?: false
            }
        } else {
            false
        }
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

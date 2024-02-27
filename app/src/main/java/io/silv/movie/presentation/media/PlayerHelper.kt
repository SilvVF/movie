package io.silv.movie.presentation.media

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.Base64
import android.view.accessibility.CaptioningManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.CaptionStyleCompat
import io.silv.movie.network.model.Streams
import org.chromium.net.CronetEngine
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.concurrent.Executors
import kotlin.math.absoluteValue


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
     * Get the system's default captions style
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getCaptionStyle(context: Context): CaptionStyleCompat {
        val captioningManager = context.getSystemService<CaptioningManager>()!!
        return if (!captioningManager.isEnabled) {
            // system captions are disabled, using android default captions style
            CaptionStyleCompat.DEFAULT
        } else {
            // system captions are enabled
            CaptionStyleCompat.createFromCaptionStyle(captioningManager.userStyle)
        }
    }

    fun getOrientation(videoWidth: Int, videoHeight: Int): Int {
        return when ("ratio") {
            "ratio" -> {
                // probably a youtube shorts video
                if (videoHeight > videoWidth) {
                    ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                } // a video with normal aspect ratio
                else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }

            "auto" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    fun getIntentAction(context: Context): String {
        return context.packageName + "." + ACTION_MEDIA_CONTROL
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

    /**
     * Check for SponsorBlock segments matching the current player position
     * @param context A main dispatcher context
     * @param segments List of the SponsorBlock segments
     * @return If segment found and should skip manually, the end position of the segment in ms, otherwise null
     */
    fun ExoPlayer.checkForSegments(
        context: Context,
        segments: List<Segment>,
        sponsorBlockConfig: MutableMap<String, SbSkipOptions>
    ): Segment? {
        for (segment in segments.filter { it.category != SPONSOR_HIGHLIGHT_CATEGORY }) {
            val (start, end) = segment.segmentStartAndEnd
            val (segmentStart, segmentEnd) = (start * 1000f).toLong() to (end * 1000f).toLong()

            // avoid seeking to the same segment multiple times, e.g. when the SB segment is at the end of the video
            if ((duration - currentPosition).absoluteValue < 500) continue

            if (currentPosition in segmentStart until segmentEnd) {
                val key = sponsorBlockConfig[segment.category]
                if (key == SbSkipOptions.AUTOMATIC ||
                    (key == SbSkipOptions.AUTOMATIC_ONCE && !segment.skipped)
                ) {
                    seekTo(segmentEnd)
                    segment.skipped = true
                } else if (key == SbSkipOptions.MANUAL ||
                    (key == SbSkipOptions.AUTOMATIC_ONCE && segment.skipped)
                ) {
                    return segment
                }
            }
        }
        return null
    }

    fun ExoPlayer.isInSegment(segments: List<Segment>): Boolean {
        return segments.any {
            val (start, end) = it.segmentStartAndEnd
            val (segmentStart, segmentEnd) = (start * 1000f).toLong() to (end * 1000f).toLong()
            currentPosition in segmentStart..segmentEnd
        }
    }


    /**
     * Get the track type string resource corresponding to ExoPlayer role flags used for audio
     * track types.
     *
     * If the role flags doesn't have any role flags used for audio track types, the string
     * resource `unknown_audio_track_type` is returned.
     *
     * @param context   a context to get the string resources used to build the audio track type
     * @param roleFlags the ExoPlayer role flags from which the audio track type will be returned
     * @return the track type string resource corresponding to an ExoPlayer role flag or the
     * `unknown_audio_track_type` one if no role flags corresponding to the ones used for audio
     * track types is set
     */
    private fun getDisplayAudioTrackTypeFromFormat(
        context: Context,
        @C.RoleFlags roleFlags: Int
    ): String {
        // These role flags should not be set together, so the first role only take into account
        // flag which matches
        return when {
            // If the flag ROLE_FLAG_DESCRIBES_VIDEO is set, return the descriptive_audio_track
            // string resource
            roleFlags and C.ROLE_FLAG_DESCRIBES_VIDEO == C.ROLE_FLAG_DESCRIBES_VIDEO ->
                "descriptive"

            // If the flag ROLE_FLAG_DESCRIBES_VIDEO is set, return the dubbed_audio_track
            // string resource
            roleFlags and C.ROLE_FLAG_DUB == C.ROLE_FLAG_DUB ->
                "dubbed"

            // If the flag ROLE_FLAG_DESCRIBES_VIDEO is set, return the original_or_main_audio_track
            // string resource
            roleFlags and C.ROLE_FLAG_MAIN == C.ROLE_FLAG_MAIN ->
                "original or main"

            // Return the unknown_audio_track_type string resource for any other value
            else -> "Unknown audio track type"
        }
    }


    /**
     * Get audio languages with their role flags of supported formats from ExoPlayer track groups
     * and only the selected ones if requested.
     *
     * Duplicate audio languages with their role flags are removed.
     *
     * @param groups                 the list of [Tracks.Group]s of the current tracks played by the player
     * @param keepOnlySelectedTracks whether to get only the selected audio languages with their
     *                               role flags among the supported ones
     * @return a list of distinct audio languages with their role flags from the supported formats
     * of the given track groups and only the selected ones if requested
     */
    fun getAudioLanguagesAndRoleFlagsFromTrackGroups(
        groups: List<Tracks.Group>,
        keepOnlySelectedTracks: Boolean
    ): List<Pair<String?, @C.RoleFlags Int>> {
        // Filter unsupported tracks and keep only selected tracks if requested
        // Use a lambda expression to avoid checking on each audio format if we keep only selected
        // tracks or not
        val trackFilter = if (keepOnlySelectedTracks) {
            { group: Tracks.Group, trackIndex: Int ->
                group.isTrackSupported(trackIndex) && group.isTrackSelected(
                    trackIndex
                )
            }
        } else {
            { group: Tracks.Group, trackIndex: Int -> group.isTrackSupported(trackIndex) }
        }

        return groups.filter {
            it.type == C.TRACK_TYPE_AUDIO
        }.flatMap { group ->
            (0 until group.length).filter {
                trackFilter(group, it)
            }.map { group.getTrackFormat(it) }
        }.map { format ->
            format.language to format.roleFlags
        }.distinct()
    }

    /**
     * Check whether the given flag is set in the given bitfield.
     *
     * @param bitField a bitfield
     * @param flag     a flag to check its presence in the given bitfield
     * @return whether the given flag is set in the given bitfield
     */
    private fun isFlagSet(bitField: Int, flag: Int) = bitField and flag == flag

    /**
     * Check whether the given ExoPlayer role flags contain at least one flag used for audio
     * track types.
     *
     * ExoPlayer role flags currently used for audio track types are [C.ROLE_FLAG_DESCRIBES_VIDEO],
     * [C.ROLE_FLAG_DUB], [C.ROLE_FLAG_MAIN] and [C.ROLE_FLAG_ALTERNATE].
     *
     * @param roleFlags the ExoPlayer role flags to check, an int representing a bitfield
     * @return whether the provided ExoPlayer flags contain a flag used for audio track types
     */
    fun haveAudioTrackRoleFlagSet(@C.RoleFlags roleFlags: Int): Boolean {
        return isFlagSet(roleFlags, C.ROLE_FLAG_DESCRIBES_VIDEO) ||
                isFlagSet(roleFlags, C.ROLE_FLAG_DUB) ||
                isFlagSet(roleFlags, C.ROLE_FLAG_MAIN) ||
                isFlagSet(roleFlags, C.ROLE_FLAG_ALTERNATE)
    }
}

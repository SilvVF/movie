package io.silv.movie

import android.net.Uri
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import io.silv.movie.data.trailers.Trailer
import io.silv.movie.data.trailers.TrailerRepository
import io.silv.movie.network.model.Streams
import io.silv.movie.network.model.Subtitle
import io.silv.movie.network.service.piped.PipedApi
import io.silv.movie.presentation.EventProducer
import io.silv.movie.presentation.media.CollapsableVideoAnchors
import io.silv.movie.presentation.media.CollapsableVideoState
import io.silv.movie.presentation.media.PlayerHelper
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.burnoutcrew.reorderable.ItemPosition
import kotlin.time.Duration.Companion.seconds


class MainViewModel(
    private val trailerRepository: TrailerRepository,
    private val pipedApi: PipedApi,
    private val savedStateHandle: SavedStateHandle
): ViewModel(), EventProducer<MainViewModel.PlayerEvent> by EventProducer.default() {

    var collapsableVideoState: CollapsableVideoState? = null

    private var trailerToStreams by mutableStateOf<Pair<Trailer, Streams>?>(null)

    var playerState by mutableIntStateOf(Player.STATE_IDLE)

    val trailerQueue = mutableStateListOf<Trailer>()

    val streams by derivedStateOf { trailerToStreams?.second }
    val currentTrailer by  derivedStateOf { trailerQueue.firstOrNull() }
    var playing by mutableStateOf(false)

    var second by mutableLongStateOf(0L)

    init {
        viewModelScope.launch {
            snapshotFlow { currentTrailer  }
                .filterNotNull()
                .distinctUntilChanged()
                .debounce {
                    if (streams == null) 0.seconds else 3.seconds
                }
                .collectLatest { trailer ->

                    if (trailer == trailerToStreams?.first)
                        return@collectLatest

                    second = 0L
                    trailerToStreams = null
                    val streams = runCatching { pipedApi.getStreams(trailer.key) }
                        .getOrNull()
                        ?: return@collectLatest

                    trailerToStreams = trailer to streams
                }
        }
    }

    private fun getSubtitleRoleFlags(subtitle: Subtitle?): Int {
        return if (subtitle?.autoGenerated != true) {
            C.ROLE_FLAG_CAPTION
        } else {
            PlayerHelper.ROLE_FLAG_AUTO_GEN_SUBTITLE
        }
    }

    private fun MediaItem.Builder.setMetadata(streams: Streams) = apply {
        setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(streams.title)
                .setArtist(streams.uploader)
                .setArtworkUri(streams.thumbnailUrl.toUri())
                .build()
        )
    }


    private fun getSubtitleConfigs(): List<MediaItem.SubtitleConfiguration> = streams!!.subtitles.map {
        val roleFlags = getSubtitleRoleFlags(it)
        MediaItem.SubtitleConfiguration.Builder(it.url!!.toUri())
            .setRoleFlags(roleFlags)
            .setLanguage(it.code)
            .setMimeType(it.mimeType).build()
    }

    fun createMediaItem(uri: Uri, mimeType: String) = MediaItem.Builder()
        .setUri(uri)
        .setMimeType(mimeType)
        .setSubtitleConfigurations(getSubtitleConfigs())
        .setMetadata(streams!!)
        .build()


    fun onMove(from: ItemPosition, to: ItemPosition) {
        if (from.index >= trailerQueue.size) { return }

        trailerQueue.add(to.index, trailerQueue.removeAt(from.index))
    }

    fun sendPlayerEvent(event: PlayerEvent) {
        viewModelScope.launch {
            emitEvent(event)
        }
    }

    fun onExoPlayerDispose(currentPosition: Long) {
        second = currentPosition
    }

    val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            when (playbackState) {
                Player.STATE_ENDED -> {
                    if (trailerQueue.size < 1) { return }

                    val t = trailerQueue.first()

                    trailerQueue.remove(t)
                    trailerQueue.add(t)
                }
                Player.STATE_BUFFERING -> {}
                Player.STATE_IDLE -> {}
                Player.STATE_READY -> {}
            }
            playerState = playbackState
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            playing = isPlaying
        }
    }


    private var contentId: Long? = savedStateHandle["content"]
        set(value) {
            savedStateHandle["content"] = value
            field = value
        }
    private var isMovie: Boolean? = savedStateHandle["content"]
        set(value) {
            savedStateHandle["content"] = value
            field = value
        }

    private var trailerId: Long?  = savedStateHandle["trailer"]
        set(value) {
            savedStateHandle["trailer"] = value
            field = value
        }

    init {
        run {
            requestMediaQueue(
                contentId ?: return@run,
                isMovie ?: return@run,
                trailerId ?: return@run
            )
        }
    }

    fun requestMediaQueue(contentId: Long, isMovie: Boolean, tid: Long) {

        this.contentId = contentId
        this.isMovie = isMovie
        trailerId = tid

        viewModelScope.launch {

            val trailers = if (isMovie) {
                trailerRepository.getTrailersByMovieId(contentId)
            } else {
                trailerRepository.getTrailersByShowId(contentId)
            }

            val mutableTrailers =  trailers.toMutableList()

            val trailerIdx = trailers.indexOfFirst { it.id == tid }

            if (trailerIdx != -1) {
                mutableTrailers.add(0, mutableTrailers.removeAt(trailerIdx))
            }

            second = 0L
            trailerToStreams = null
            trailerQueue.clear()
            trailerQueue.addAll(mutableTrailers.toImmutableList())
            withContext(Dispatchers.Main) {
                collapsableVideoState?.state?.snapTo(CollapsableVideoAnchors.Start)
            }
        }
    }

    fun clearMediaQueue() {
        second = 0L
        trailerQueue.clear()
        viewModelScope.launch {
            collapsableVideoState?.state?.snapTo(CollapsableVideoAnchors.Dismiss)
        }
    }

    sealed interface PlayerEvent {
        data object Pause: PlayerEvent
        data object Play: PlayerEvent
        data object Mute: PlayerEvent
    }
}

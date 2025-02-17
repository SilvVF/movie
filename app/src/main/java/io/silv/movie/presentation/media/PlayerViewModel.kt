package io.silv.movie.presentation.media

import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import io.silv.movie.data.model.Trailer
import io.silv.movie.data.local.TrailerRepository
import io.silv.movie.network.model.Streams
import io.silv.movie.network.model.Subtitle
import io.silv.movie.network.service.piped.PipedApi
import io.silv.movie.presentation.EventProducer
import io.silv.movie.presentation.media.util.PlayerHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ItemPosition
import timber.log.Timber

sealed interface StreamState {
    data object Loading: StreamState
    data class Success(val streams: Streams): StreamState
    data class Failure(val message: String): StreamState
}

class PlayerViewModel(
    private val trailerRepository: TrailerRepository,
    private val pipedApi: PipedApi,
    private val savedStateHandle: SavedStateHandle,
): ViewModel(), EventProducer<PlayerViewModel.PlayerEvent> by EventProducer.default() {

    private var trailerToStreams by mutableStateOf<Pair<Trailer, StreamState>?>(null)

    var playerState by mutableIntStateOf(Player.STATE_IDLE)

    val trailerQueue = mutableStateListOf<Trailer>()

    val trailer by derivedStateOf { trailerToStreams?.first }
    val streamState by derivedStateOf { trailerToStreams?.second }
    val streams by derivedStateOf { (streamState as? StreamState.Success)?.streams }
    val currentTrailer by  derivedStateOf { trailerQueue.firstOrNull() }
    var playing by mutableStateOf(false)

    val secondToStream = mutableStateMapOf<String, Long>()

    init {
        run {
            requestMediaQueue(
                contentId ?: return@run,
                isMovie ?: return@run,
                trailerId ?: return@run
            )
        }
        viewModelScope.launch {
            snapshotFlow { currentTrailer to trailerToStreams }
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { (trailer, tTos) ->

                    if (trailer == tTos?.first || trailer == null)
                        return@collectLatest

                    trailerToStreams = null
                    trailerToStreams = trailer to runCatching { pipedApi.getStreams(trailer.key) }
                        .fold(
                            onFailure = {
                                Timber.e(it)
                                StreamState.Failure("Error loading video piped may be down")
                            },
                            onSuccess = {
                                StreamState.Success(it)
                            }
                        )
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
        val fromIdx = from.index
        val toIdx = to.index
        if (
            fromIdx < 0 ||
            toIdx < 0 ||
            fromIdx > trailerQueue.lastIndex ||
            toIdx > trailerQueue.lastIndex
        ) {
           return
        }

        trailerQueue.add(toIdx, trailerQueue.removeAt(fromIdx))
    }

    fun sendPlayerEvent(event: PlayerEvent) {
        viewModelScope.launch {
            emitEvent(event)
        }
    }

    fun onExoPlayerDispose(currentPosition: Long) {
        trailerToStreams?.let {
            secondToStream[it.first.id] = currentPosition
        }
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
    private var isMovie: Boolean? = savedStateHandle["isMovie"]
        set(value) {
            savedStateHandle["isMovie"] = value
            field = value
        }

    private var trailerId: String?  = savedStateHandle["trailer"]
        set(value) {
            savedStateHandle["trailer"] = value
            field = value
        }

    fun requestMediaQueue(contentId: Long, isMovie: Boolean, tid: String) {
        clearMediaQueue(true)

        this.contentId = contentId
        this.isMovie = isMovie
        trailerId = tid

        viewModelScope.launch {
            val trailers = if (isMovie) {
                trailerRepository.getByMovieId(contentId)
            } else {
                trailerRepository.getByShowId(contentId)
            }

            val mutableTrailers =  trailers.toMutableList()

            val trailerIdx = trailers.indexOfFirst { it.id == tid }

            if (trailerIdx != -1) {
                mutableTrailers.add(0, mutableTrailers.removeAt(trailerIdx))
            }

            trailerToStreams = null
            trailerQueue.clear()
            trailerQueue.addAll(mutableTrailers)
        }
    }


    fun clearMediaQueue(clearSavedStateForContent: Boolean = false) {
        if (clearSavedStateForContent) {
            savedStateHandle["item"] = null
        }
        trailerToStreams = null
        trailerQueue.clear()
    }

    sealed interface PlayerEvent {
        data object Pause: PlayerEvent
        data object Play: PlayerEvent
        data object Mute: PlayerEvent
    }
}

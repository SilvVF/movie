package io.silv.movie.presentation.media

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import io.silv.movie.data.model.Trailer
import io.silv.movie.data.local.TrailerRepository
import io.silv.movie.api.model.Streams
import io.silv.movie.api.service.piped.PipedApi
import io.silv.movie.core.STrailer
import io.silv.movie.core.onEachLatest
import io.silv.movie.presentation.EventProducer
import io.silv.movie.presentation.media.components.CollapsableVideoAnchors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.burnoutcrew.reorderable.ItemPosition
import timber.log.Timber

sealed interface StreamState {
    data object Idle : StreamState

    data class Loading(val trailer: Trailer) : StreamState
    data class Success(val trailer: Trailer, val streams: Streams) : StreamState
    data class Failure(val trailer: Trailer, val message: String) : StreamState

    fun trailerOrNull() = when (this) {
        is Failure -> trailer
        Idle -> null
        is Loading -> trailer
        is Success -> trailer
    }
}

data class PresenterState(
    val streamState: StreamState = StreamState.Idle,
    val playing: Boolean,
    val playerState: Int,
    val queue: List<Trailer>
)

class PlayerPresenter(
    private val trailerRepository: TrailerRepository,
    private val pipedApi: PipedApi,
    private val scope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
) : EventProducer<PlayerPresenter.PlayerEvent> by EventProducer.default() {

    val dataSourceFactory = OkHttpDataSource.Factory(OkHttpClient.Builder().build())

    val initialVideoAnchor = savedStateHandle.get<CollapsableVideoAnchors>("initial_video_anchor")

    private val streamState = MutableStateFlow<StreamState>(StreamState.Idle)
    private val playerState = MutableStateFlow(Player.STATE_IDLE)
    private val playing = MutableStateFlow(false)

    private val trailerQueue = MutableStateFlow<List<Trailer>>(emptyList())

    val presenterState = combine(
        trailerQueue.asStateFlow(),
        streamState.asStateFlow(),
        playerState.asStateFlow(),
        playing.asStateFlow(),
    ) { queue, stream, player, playing ->
        PresenterState(
            streamState = stream,
            playing = playing,
            playerState = player,
            queue = queue
        )
    }
        .stateIn(
            scope,
            SharingStarted.WhileSubscribed(5_000),
            PresenterState(streamState.value, playing.value, playerState.value, trailerQueue.value)
        )

    val secondToStream = savedStateHandle.get<Map<String, Long>>("saved_pos")
        ?.toMutableMap()
        ?: mutableMapOf()

    init {
        run {
            requestMediaQueue(
                contentId ?: return@run,
                isMovie ?: return@run,
                trailerId ?: return@run
            )
        }
        presenterState.onEach { state ->
            if (
                state.queue.isEmpty() ||
                state.queue.first() == state.streamState.trailerOrNull()
            ) {
                return@onEach
            }

            val trailer = state.queue.first()
            streamState.emit(StreamState.Loading(trailer))
            val stream = pipedApi.getStreams(trailer.key)
                .fold(
                    onFailure = {
                        Timber.e(it)
                        StreamState.Failure(trailer, "Error loading video piped may be down")
                    },
                    onSuccess = {
                        StreamState.Success(trailer, it)
                    }
                )
            streamState.emit(stream)
        }
            .launchIn(scope)
    }


    fun onMove(from: ItemPosition, to: ItemPosition) {
        trailerQueue.update { queue ->
            val fromIdx = from.index
            val toIdx = to.index
            if (
                fromIdx < 0 ||
                toIdx < 0 ||
                fromIdx > queue.lastIndex ||
                toIdx > queue.lastIndex
            ) {
                queue
            } else {
                queue.toMutableList().apply {
                    add(toIdx, removeAt(fromIdx))
                }
            }
        }
    }

    fun sendPlayerEvent(event: PlayerEvent) {
        scope.launch {
            emitEvent(event)
        }
    }

    val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            when (playbackState) {
                Player.STATE_ENDED -> {
                    confirmedReorder.update { null }
                    trailerQueue.update { queue ->
                        if (queue.isEmpty()) {
                            queue
                        } else {
                            val t = queue.first()
                            queue.toMutableList().apply {
                                remove(t)
                                add(t)
                            }
                        }
                    }
                }

                Player.STATE_BUFFERING -> {}
                Player.STATE_IDLE -> {}
                Player.STATE_READY -> {}
            }
            playerState.update { playbackState }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            playing.update { isPlaying }
        }
    }

    fun saveState(pos: Long, anchors: CollapsableVideoAnchors) {
        (streamState.value as? StreamState.Success)?.let {
            secondToStream[it.trailer.id] = pos
            savedStateHandle["saved_pos"] = secondToStream
        }
        savedStateHandle["initial_video_anchor"] = initialVideoAnchor
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

    private var trailerId: String? = savedStateHandle["trailer"]
        set(value) {
            savedStateHandle["trailer"] = value
            field = value
        }

    fun requestMediaQueue(contentId: Long, isMovie: Boolean, tid: String) {
        clearMediaQueue(true)

        this.contentId = contentId
        this.isMovie = isMovie
        trailerId = tid

        scope.launch {
            val trailers = if (isMovie) {
                trailerRepository.getByMovieId(contentId)
            } else {
                trailerRepository.getByShowId(contentId)
            }

            val mutableTrailers = trailers.toMutableList()

            val trailerIdx = trailers.indexOfFirst { it.id == tid }

            if (trailerIdx != -1) {
                mutableTrailers.add(0, mutableTrailers.removeAt(trailerIdx))
            }

            streamState.emit(StreamState.Idle)
            trailerQueue.emit(emptyList())
            trailerQueue.emit(mutableTrailers)
        }
    }


    fun clearMediaQueue(clearSavedStateForContent: Boolean = true) {
        if (clearSavedStateForContent) {
            savedStateHandle.keys().forEach {
                savedStateHandle[it] = null
            }
        }
        streamState.update { StreamState.Idle }
        trailerQueue.update { emptyList() }
    }

    sealed interface PlayerEvent {
        data object Pause : PlayerEvent
        data object Play : PlayerEvent
        data object Mute : PlayerEvent
        data class SnapTo(val anchors: CollapsableVideoAnchors) : PlayerEvent
    }
}

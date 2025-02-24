package io.silv.movie.presentation.media

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import io.silv.movie.IoDispatcher
import io.silv.movie.api.model.Streams
import io.silv.movie.api.service.piped.PipedApi
import io.silv.movie.data.local.TrailerRepository
import io.silv.movie.data.model.Trailer
import io.silv.movie.presentation.EventProducer
import io.silv.movie.presentation.media.components.CollapsableVideoAnchors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
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
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : EventProducer<PlayerPresenter.PlayerEvent> by EventProducer.default() {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    val dataSourceFactory = OkHttpDataSource.Factory(OkHttpClient.Builder().build())

    var initialVideoAnchor: CollapsableVideoAnchors? = null
    private var contentId: Long? = null
    private var isMovie: Boolean? = null
    private var trailerId: String? = null

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

    val secondToStream = mutableMapOf<String, Long>()

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
                        StreamState.Failure(trailer, "Error loading video")
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
            queue.toTypedArray().apply {
                val temp = this[to.index]

                this[to.index] = queue[from.index]
                this[from.index] = temp
            }
                .toList()
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

                else -> Unit
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
        }
        initialVideoAnchor = anchors
    }

    fun requestMediaQueue(contentId: Long, isMovie: Boolean, tid: String) {
        clearMediaQueue()

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


    fun clearMediaQueue() {
        streamState.value = StreamState.Idle
        trailerQueue.value = emptyList()

        secondToStream.clear()
        initialVideoAnchor = null
        contentId = null
        isMovie = null
        trailerId = null
    }

    sealed interface PlayerEvent {
        data object Pause : PlayerEvent
        data object Play : PlayerEvent
        data object Mute : PlayerEvent
    }
}

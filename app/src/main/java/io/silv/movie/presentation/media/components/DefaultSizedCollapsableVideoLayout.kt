package io.silv.movie.presentation.media.components

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.animateToWithDecay
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import io.silv.core_ui.components.bottomsheet.modal.PredictiveBack
import io.silv.core_ui.components.lazy.FastScrollLazyColumn
import io.silv.core_ui.util.clickableNoIndication
import io.silv.movie.R
import io.silv.movie.api.model.Streams
import io.silv.movie.api.model.Subtitle
import io.silv.movie.core.onEachLatest
import io.silv.movie.data.model.Trailer
import io.silv.movie.presentation.media.PlayerPresenter
import io.silv.movie.presentation.media.PlayerPresenter.PlayerEvent
import io.silv.movie.presentation.media.StreamState
import io.silv.movie.presentation.media.util.DashHelper
import io.silv.movie.presentation.media.util.PlayerHelper
import io.silv.movie.presentation.media.util.YoutubeHlsPlaylistParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ItemPosition
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import kotlin.math.roundToInt

enum class CollapsableVideoAnchors {
    Start,
    End,
    Dismiss
}

val CollapsablePlayerMinHeight = 72.dp

@OptIn(UnstableApi::class)
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

@OptIn(UnstableApi::class)
class VideoState(
    private val presenter: PlayerPresenter,
    private val scope: CoroutineScope,
    context: Context,
    snackbarHostState: SnackbarHostState? = null,
) {

    val player = createExoPlayer(context, presenter.playerListener)

    private val density = Density(context)
    private val configuration = context.resources.configuration

    val state = presenter.presenterState

    val fullScreenDragState = AnchoredDraggableState(
        initialValue = VideoDragAnchors.Normal,
        anchors = DraggableAnchors {
            VideoDragAnchors.Normal at 0f
            VideoDragAnchors.FullScreen at with(density) { configuration.screenHeightDp.dp.toPx() / 2 }
        },
        positionalThreshold = { distance: Float -> distance * 0.5f },
        velocityThreshold = { with(density) { 100.dp.toPx() } },
        snapAnimationSpec = tween(),
        decayAnimationSpec = splineBasedDecay(density),
    )

    val videoDragState = AnchoredDraggableState(
        initialValue = presenter.initialVideoAnchor ?: CollapsableVideoAnchors.Dismiss,
        anchors = DraggableAnchors {
            CollapsableVideoAnchors.Start at 0f
            CollapsableVideoAnchors.End at
                    with(density) { configuration.screenHeightDp.dp.toPx() }
            CollapsableVideoAnchors.Dismiss at
                    with(density) { configuration.screenHeightDp.dp.toPx() + CollapsablePlayerMinHeight.toPx() }
        },
        positionalThreshold = { distance: Float -> distance * 0.5f },
        velocityThreshold = { with(density) { 100.dp.toPx() } },
        snapAnimationSpec = tween(),
        decayAnimationSpec = splineBasedDecay(density),
    )


    val fullScreenDragEnabled by derivedStateOf {
        fullScreenDragState.currentValue == VideoDragAnchors.FullScreen
    }

    val progress by derivedStateOf {
        1 - (videoDragState.requireOffset() / videoDragState.anchors.positionOf(
            CollapsableVideoAnchors.End
        ))
            .coerceIn(0f..1f)
    }

    val fullScreenProgress by derivedStateOf {
        val fullscreenAnchorPos =
            fullScreenDragState.anchors.positionOf(VideoDragAnchors.FullScreen)
        (fullScreenDragState.requireOffset() / fullscreenAnchorPos)
            .coerceIn(0f..1f)
    }

    val dismissOffsetPx by derivedStateOf {
        val offset = videoDragState.requireOffset() - videoDragState.anchors.positionOf(
            CollapsableVideoAnchors.End
        )
        offset.coerceAtLeast(0f).roundToInt()
    }

    val dismissFullscreenOffsetPx by derivedStateOf {
        val offset = fullScreenDragState.requireOffset() - fullScreenDragState.anchors.positionOf(
            VideoDragAnchors.FullScreen
        )
        offset.coerceAtLeast(0f).roundToInt()
    }

    val bottomPadding by derivedStateOf {
        with(density) {
            CollapsablePlayerMinHeight - dismissOffsetPx.toDp()
        }
    }

    init {
        scope.launch {
            try {
                awaitCancellation()
            } finally {
                player.release()
            }
        }

        presenter.presenterState
            .map { it.queue.isNotEmpty() }
            .distinctUntilChanged()
            .onEach {
                if (it && videoDragState.currentValue == CollapsableVideoAnchors.Dismiss) {
                    videoDragState.animateTo(CollapsableVideoAnchors.Start)
                } else {
                    videoDragState.animateTo(CollapsableVideoAnchors.End)
                }
        }
            .launchIn(scope)

        presenter.eventsAsFlow().onEach { event ->
            when (event) {
                PlayerEvent.Pause -> player.pause()
                PlayerEvent.Play -> player.play()
                PlayerEvent.Mute -> player.setDeviceMuted(
                    true,
                    C.VOLUME_FLAG_REMOVE_SOUND_AND_VIBRATE
                )
            }
        }
            .launchIn(scope)

        snapshotFlow { videoDragState.currentValue }.onEach { anchors ->
            presenter.saveState(player.currentPosition, anchors)
            if (anchors == CollapsableVideoAnchors.Dismiss) {
                presenter.clearMediaQueue()
            }
        }
            .launchIn(scope)


        presenter.presenterState
            .map { it.streamState }
            .distinctUntilChanged()
            .onEachLatest { state ->
                when (state) {
                    is StreamState.Failure -> {
                        snackbarHostState?.showSnackbar(
                            message = context.getString(R.string.player_error),
                            duration = SnackbarDuration.Short
                        )
                    }

                    is StreamState.Success -> {
                        if (state.streams.hls == null) {
                            snackbarHostState?.showSnackbar(
                                message = context.getString(R.string.player_error),
                                duration = SnackbarDuration.Short
                            )
                        } else {
                            val hlsMediaSourceFactory =
                                HlsMediaSource.Factory(presenter.dataSourceFactory)
                                    .setPlaylistParserFactory(YoutubeHlsPlaylistParser.Factory())

                            val mediaSource = hlsMediaSourceFactory.createMediaSource(
                                state.streams.createMediaItem(
                                    DashHelper.unwrapUrl(state.streams.hls).toUri(),
                                    MimeTypes.APPLICATION_M3U8
                                )
                            )
                            player.setMediaSource(mediaSource)
                            player.seekTo(presenter.secondToStream[state.trailer.id] ?: 0L)
                            player.playWhenReady = true
                            player.prepare()
                        }
                    }

                    else -> Unit
                }
            }
            .launchIn(scope)
    }

    fun onMove(from: ItemPosition, to: ItemPosition) {
        presenter.onMove(from, to)
    }

    fun sendPlayerEvent(event: PlayerEvent) {
        presenter.sendPlayerEvent(event)
    }

    fun clearQueue() {
        presenter.clearMediaQueue()
    }

    fun settle() {
        scope.launch {
            videoDragState.settle(videoDragState.lastVelocity)
        }
    }

    fun predictiveBack(progress: Float) {
        val targetOffset = videoDragState.anchors.minAnchor() + lerp(
            0f,
            videoDragState.anchors.maxAnchor() * 0.24f,
            PredictiveBack.transform(progress)
        )

        val dist = targetOffset - videoDragState.offset
        scope.launch {
            videoDragState.dispatchRawDelta(
                dist
            )
        }
    }

    fun expand() {
        scope.launch {
            videoDragState.animateTo(CollapsableVideoAnchors.Start)
        }
    }

    fun dismiss() {
        scope.launch {
            videoDragState.animateTo(CollapsableVideoAnchors.Dismiss)
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


private fun Streams.getSubtitleConfigs(): List<MediaItem.SubtitleConfiguration> = subtitles.map {
    val roleFlags = getSubtitleRoleFlags(it)
    MediaItem.SubtitleConfiguration.Builder(it.url!!.toUri())
        .setRoleFlags(roleFlags)
        .setLanguage(it.code)
        .setMimeType(it.mimeType).build()
}

fun Streams.createMediaItem(uri: Uri, mimeType: String) = MediaItem.Builder()
    .setUri(uri)
    .setMimeType(mimeType)
    .setSubtitleConfigurations(getSubtitleConfigs())
    .setMetadata(this)
    .build()


private class CollapsableVideoLayoutScrollConnection(
    private val lazyListState: LazyListState,
    private val state: AnchoredDraggableState<VideoDragAnchors>,
    scope: CoroutineScope,
) : NestedScrollConnection {

    var canConsumeDelta by mutableStateOf(true)
    var preFlingIdx by mutableIntStateOf(0)

    init {
        // block nested scrolling when the user scrolls the list up
        // this prevents flinging to the start animating the player to take the screen
        scope.launch {
            snapshotFlow { lazyListState.isScrollInProgress }.collectLatest { scrolling ->

                canConsumeDelta = lazyListState.firstVisibleItemIndex <= 0

                if (!scrolling) {
                    return@collectLatest
                }


                while (lazyListState.firstVisibleItemIndex <= 0) {
                    delay(1)
                }

                canConsumeDelta = lazyListState.firstVisibleItemIndex <= 0
            }
        }
    }

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val delta = available.y
        return if (delta < 0 && canConsumeDelta && lazyListState.firstVisibleItemIndex == 0) {
            Offset(
                x = available.x,
                y = state.dispatchRawDelta(delta)
            )
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val delta = available.y
        return Offset(
            x = available.x,
            y = if (canConsumeDelta && lazyListState.firstVisibleItemIndex == 0 && preFlingIdx == 0)
                state.dispatchRawDelta(delta)
            else
                available.y
        )
    }

    override suspend fun onPreFling(available: Velocity): Velocity {

        preFlingIdx = lazyListState.firstVisibleItemIndex

        return if (available.y < 0 && !lazyListState.canScrollBackward && canConsumeDelta && lazyListState.firstVisibleItemIndex == 0) {
            state.animateToWithDecay(state.targetValue, available.y)
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity
    ): Velocity {
        state.animateToWithDecay(state.targetValue, available.y)
        return super.onPostFling(consumed, available)
    }
}

enum class VideoDragAnchors {
    Normal, FullScreen
}

@Composable
fun DefaultSizeCollapsableVideoLayout(
    modifier: Modifier = Modifier,
    reorderState: ReorderableLazyListState,
    player: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    videoState: VideoState,
    scrollToTopButton: @Composable (triggerScroll: () -> Unit) -> Unit,
    pinnedContent: @Composable () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    val progress = videoState.progress
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()


    val topPadding = WindowInsets.systemBars.getTop(density)
    val bottomPadding = WindowInsets.systemBars.getBottom(density)

    val actionsAlpha = lerp(1f, 0f, progress / 0.1f)
    val contentAlpha = lerp(0f, 1f, progress / 0.8f)

    val nestedScrollConnection =
        remember(reorderState.listState, videoState.fullScreenDragState) {
            CollapsableVideoLayoutScrollConnection(
                reorderState.listState,
                videoState.fullScreenDragState,
                scope
            )
        }

    Layout(
        {
            Row(
                modifier = Modifier
                    .graphicsLayer { alpha = actionsAlpha }
                    .padding(horizontal = 4.dp)
                    .layoutId("actions"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                actions()
            }
            Box(
                modifier = Modifier
                    .height(IntrinsicSize.Max)
                    .aspectRatio(16f / 9f)
                    .anchoredDraggable(
                        videoState.videoDragState,
                        Orientation.Vertical,
                        !videoState.fullScreenDragEnabled
                    )
                    .anchoredDraggable(
                        videoState.fullScreenDragState,
                        Orientation.Vertical,
                        videoState.fullScreenDragEnabled,
                    )
                    .layoutId("player")
            ) {
                player()
            }
            Surface(
                modifier = Modifier
                    .layoutId("content")
                    .graphicsLayer {
                        alpha =
                            minOf(
                                1f - videoState.fullScreenProgress,
                                contentAlpha
                            )
                    },
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                LazyColumnAnimatedAlpha(
                    reorderState = reorderState,
                    nestedScrollConnection = nestedScrollConnection,
                    scrollEnabled = !videoState.fullScreenDragEnabled,
                    bottomPaddingPx = bottomPadding
                ) {
                    stickyHeader("PINNED_CONTENT") {
                        pinnedContent()
                    }
                    content()
                }
            }
            Box(modifier = Modifier
                .graphicsLayer { alpha = 1f - videoState.fullScreenProgress }
                .layoutId("scrollToTop")
            ) {
                val visible by remember {
                    derivedStateOf { reorderState.listState.firstVisibleItemIndex > 0 }
                }
                AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
                    scrollToTopButton {
                        scope.launch { reorderState.listState.animateScrollToItem(0) }
                    }
                }
            }
        },
        modifier = modifier
            .wrapContentSize()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(BottomSheetDefaults.Elevation))
            .anchoredDraggable(
                videoState.fullScreenDragState,
                Orientation.Vertical,
                videoState.fullScreenDragEnabled
            )
            .anchoredDraggable(
                videoState.videoDragState,
                Orientation.Vertical,
                enabled = videoState.fullScreenDragState.currentValue == VideoDragAnchors.Normal
            )
            .clickableNoIndication {
                if (progress < 0.1f) {
                    videoState.expand()
                }
            }
    ) { measurables, constraints ->

        val height = lerp(CollapsablePlayerMinHeight.roundToPx(), constraints.maxHeight, progress)
        val paddingTop = (topPadding * progress).roundToInt()

        val landscape =
            constraints.maxWidth > constraints.maxHeight && videoState.videoDragState.currentValue != CollapsableVideoAnchors.Dismiss

        val playerPlaceable = measurables
            .first { it.layoutId == "player" }
            .measure(
                constraints.copy(
                    maxHeight = height - if (landscape) lerp(
                        0,
                        bottomPadding,
                        progress
                    ) else 0
                )
            )

        val buttonPlaceable = measurables
            .first { it.layoutId == "scrollToTop" }
            .measure(constraints)

        val contentPlaceable = measurables
            .first { it.layoutId == "content" }
            .measure(
                constraints.copy(
                    maxHeight = (height - playerPlaceable.height - paddingTop).coerceAtLeast(
                        0
                    )
                )
            )

        val actionsPlaceable = measurables
            .first { it.layoutId == "actions" }
            .measure(constraints.copy(maxWidth = constraints.maxWidth - playerPlaceable.width))

        videoState.fullScreenDragState.updateAnchors(
            DraggableAnchors {
                VideoDragAnchors.Normal at 0f
                VideoDragAnchors.FullScreen at 1200f
            }
        )

        layout(
            constraints.maxWidth,
            (height - videoState.dismissOffsetPx)
                .coerceAtLeast(0)
        ) {

            val playerCenteredY = (constraints.maxHeight / 2f - playerPlaceable.height + paddingTop)
            val playerY = (playerCenteredY * videoState.fullScreenProgress).roundToInt()

            if (landscape) {
                val playerX =
                    lerp(0, constraints.maxWidth / 2 - playerPlaceable.width / 2, progress)
                playerPlaceable.placeRelative(
                    playerX,
                    paddingTop + playerY.coerceAtLeast(0) + videoState.dismissFullscreenOffsetPx
                )

                actionsPlaceable.placeRelative(
                    constraints.maxWidth - actionsPlaceable.width,
                    (playerPlaceable.height / 2) - (actionsPlaceable.height / 2)
                )
            } else {
                actionsPlaceable.placeRelative(
                    constraints.maxWidth - actionsPlaceable.width,
                    (playerPlaceable.height / 2) - (actionsPlaceable.height / 2)
                )

                val contentY = paddingTop + playerY + playerPlaceable.height

                if (contentY < constraints.maxHeight) {
                    contentPlaceable.placeRelative(
                        0,
                        contentY
                    )
                }

                playerPlaceable.placeRelative(
                    0,
                    paddingTop + playerY.coerceAtLeast(0) + videoState.dismissFullscreenOffsetPx
                )

                if (progress == 1f) {
                    buttonPlaceable.placeRelative(
                        x = constraints.maxWidth / 2 - buttonPlaceable.width / 2,
                        y = constraints.maxHeight - buttonPlaceable.height - bottomPadding
                    )
                }
            }
        }
    }
}

@Composable
private fun LazyColumnAnimatedAlpha(
    reorderState: ReorderableLazyListState,
    nestedScrollConnection: NestedScrollConnection,
    scrollEnabled: Boolean,
    bottomPaddingPx: Int,
    content: LazyListScope.() -> Unit,
) {
    val density = LocalDensity.current
    FastScrollLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .reorderable(reorderState)
            .nestedScroll(nestedScrollConnection),
        state = reorderState.listState,
        userScrollEnabled = scrollEnabled
    ) {
        content()
        item {
            Spacer(
                Modifier.height(
                    with(density) {
                        bottomPaddingPx.toDp() + 42.dp
                    }
                )
            )
        }
    }
}
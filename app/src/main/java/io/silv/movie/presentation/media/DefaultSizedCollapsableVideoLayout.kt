package io.silv.movie.presentation.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.silv.core_ui.components.lazy.FastScrollLazyColumn
import io.silv.core_ui.util.clickableNoIndication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import kotlin.math.roundToInt

enum class CollapsableVideoAnchors {
    Start,
    End,
    Dismiss
}

val CollapsablePlayerMinHeight = 72.dp

@Composable
fun rememberCollapsableVideoState(): CollapsableVideoState {

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

     var initial by rememberSaveable {
         mutableStateOf(CollapsableVideoAnchors.Start)
     }

    val state = remember {
        AnchoredDraggableState(
            initialValue = initial,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            animationSpec = tween(),
        ).apply {
            updateAnchors(
                DraggableAnchors {
                    CollapsableVideoAnchors.Start at 0f
                    CollapsableVideoAnchors.End at
                            with(density) { configuration.screenHeightDp.dp.toPx() }
                    CollapsableVideoAnchors.Dismiss at
                            with(density) { configuration.screenHeightDp.dp.toPx() + CollapsablePlayerMinHeight.toPx() }
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { state.currentValue }
            .collectLatest { initial = it }
    }

    val fullScreenVideoDraggable = remember {
        AnchoredDraggableState(
            initialValue = VideoDragAnchors.Normal,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            animationSpec = tween(),
        ).apply {
            updateAnchors(
                DraggableAnchors {
                    VideoDragAnchors.Normal at 0f
                    VideoDragAnchors.FullScreen at 1200f
                    VideoDragAnchors.Dismiss at 1200f
                }
            )
        }
    }

    val scope =  rememberCoroutineScope()

    return remember { CollapsableVideoState(state, fullScreenVideoDraggable, scope) }
}

class CollapsableVideoState(
    val state: AnchoredDraggableState<CollapsableVideoAnchors>,
    val fullscreenState: AnchoredDraggableState<VideoDragAnchors>,
    private val scope: CoroutineScope
) {
    val progress by derivedStateOf {
        1 - (state.requireOffset() / state.anchors.positionOf(CollapsableVideoAnchors.End))
            .coerceIn(0f..1f)
    }

    val fullScreenProgress by derivedStateOf {
            val fullscreenAnchorPos = fullscreenState.anchors.positionOf(VideoDragAnchors.FullScreen)
            (fullscreenState.requireOffset() / fullscreenAnchorPos)
                .coerceIn(0f..1f)
    }

    val dismissOffsetPx  by derivedStateOf {
        val offset = state.requireOffset() - state.anchors.positionOf(CollapsableVideoAnchors.End)
        offset.coerceAtLeast(0f).roundToInt()
    }

    val dismissFullscreenOffsetPx  by derivedStateOf {
        val offset = fullscreenState.requireOffset() - fullscreenState.anchors.positionOf(
            VideoDragAnchors.FullScreen
        )
        offset.coerceAtLeast(0f).roundToInt()
    }

    fun expand() {
        scope.launch {
            state.animateTo(CollapsableVideoAnchors.Start)
        }
    }

    fun dismiss() {
        scope.launch {
            state.animateTo(CollapsableVideoAnchors.Dismiss)
        }
    }
}

private class CollapsableVideoLayoutScrollConnection(
    private val lazyListState: LazyListState,
    private val state: AnchoredDraggableState<VideoDragAnchors>,
    scope: CoroutineScope,
): NestedScrollConnection {

    var allowedToScroll = true
    var preFlingIdx = 0

    init {
        // block nested scrolling when the user scrolls the list up
        // this prevents flinging to the start animating the player to take the screen
        scope.launch {
            snapshotFlow { lazyListState.isScrollInProgress }.collectLatest { scrolling ->

                allowedToScroll = lazyListState.firstVisibleItemIndex <= 0

                if (!scrolling) {
                    return@collectLatest
                }


                while (lazyListState.firstVisibleItemIndex <= 0) {
                    ensureActive()
                    delay(3)
                }

                ensureActive()

                allowedToScroll = lazyListState.firstVisibleItemIndex <= 0
            }
        }
    }

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val delta = available.y
        return if (delta < 0 && allowedToScroll && lazyListState.firstVisibleItemIndex == 0) {
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
            y = if (allowedToScroll && lazyListState.firstVisibleItemIndex == 0 && preFlingIdx == 0)
                state.dispatchRawDelta(delta)
            else
                available.y
        )
    }

    override suspend fun onPreFling(available: Velocity): Velocity {

        preFlingIdx = lazyListState.firstVisibleItemIndex

        return if (available.y < 0 && !lazyListState.canScrollBackward && allowedToScroll && lazyListState.firstVisibleItemIndex == 0) {
            state.animateTo(state.targetValue.takeIf { it != VideoDragAnchors.Dismiss } ?: VideoDragAnchors.FullScreen, available.y)
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity
    ): Velocity {
        state.animateTo(state.targetValue.takeIf { it != VideoDragAnchors.Dismiss } ?: VideoDragAnchors.FullScreen, available.y)
        return super.onPostFling(consumed, available)
    }
}

enum class VideoDragAnchors {
    Normal, FullScreen, Dismiss
}

@Composable
fun DefaultSizeCollapsableVideoLayout(
    onDismissRequested: () -> Unit,
    modifier: Modifier = Modifier,
    reorderState: ReorderableLazyListState,
    player: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    collapsableVideoState: CollapsableVideoState = rememberCollapsableVideoState(),
    scrollToTopButton: @Composable (triggerScroll: () -> Unit) -> Unit,
    content: LazyListScope.() -> Unit,
) {
    val progress = collapsableVideoState.progress
    val density= LocalDensity.current
    val state = collapsableVideoState.state
    val scope = rememberCoroutineScope()

    LaunchedEffect(state) {
        snapshotFlow { state.currentValue }.collect {
            if (state.currentValue == CollapsableVideoAnchors.Dismiss) {
                onDismissRequested()
            }
        }
    }

    LaunchedEffect(collapsableVideoState.fullscreenState) {
        snapshotFlow { collapsableVideoState.fullscreenState.currentValue }.collect {
            if (collapsableVideoState.fullscreenState.currentValue == VideoDragAnchors.Dismiss) {
                collapsableVideoState.fullscreenState.animateTo(VideoDragAnchors.Normal)
            }
        }
    }

    val fullscreenDraggableEnabled by remember {
        derivedStateOf {
            collapsableVideoState.fullscreenState.currentValue == VideoDragAnchors.FullScreen
        }
    }

    val topPadding = WindowInsets.systemBars.getTop(density)
    val bottomPadding = WindowInsets.systemBars.getBottom(density)

    val actionsAlpha = lerp(1f, 0f, progress / 0.1f)
    val contentAlpha = lerp(0f, 1f, progress / 0.8f)

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
            Box(modifier = Modifier
                .height(IntrinsicSize.Max)
                .aspectRatio(16f / 9f)
                .anchoredDraggable(
                    collapsableVideoState.fullscreenState,
                    Orientation.Vertical,
                    fullscreenDraggableEnabled
                )
                .anchoredDraggable(
                    state,
                    Orientation.Vertical,
                    !fullscreenDraggableEnabled
                )
                .layoutId("player")
            ) {
                player()
            }
            Surface(modifier = Modifier
                .layoutId("content")
                .anchoredDraggable(
                    collapsableVideoState.fullscreenState,
                    Orientation.Vertical,
                    fullscreenDraggableEnabled
                ),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {

                val nestedScrollConnection =
                    remember(reorderState.listState, collapsableVideoState.fullscreenState) {
                    CollapsableVideoLayoutScrollConnection(
                        reorderState.listState,
                        collapsableVideoState.fullscreenState,
                        scope
                    )
                }

                FastScrollLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha =
                                minOf(1f - collapsableVideoState.fullScreenProgress, contentAlpha)
                        }
                        .reorderable(reorderState)
                        .nestedScroll(nestedScrollConnection),
                    state = reorderState.listState,
                    userScrollEnabled = !fullscreenDraggableEnabled
                ) {
                    content()
                    item {
                        Spacer(
                            Modifier.height(
                                with(density) {
                                    bottomPadding.toDp() + 42.dp
                                }
                            )
                        )
                    }
                }
            }
            Box(modifier = Modifier
                .graphicsLayer {
                    alpha = 1f - collapsableVideoState.fullScreenProgress
                }
                .layoutId("scrollToTop")
            ) {
                val visible by remember {
                    derivedStateOf { reorderState.listState.firstVisibleItemIndex > 0 }
                }
                AnimatedVisibility(visible = visible, enter = fadeIn(),exit = fadeOut()) {
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
                collapsableVideoState.fullscreenState,
                Orientation.Vertical,
                fullscreenDraggableEnabled
            )
            .anchoredDraggable(
                state,
                Orientation.Vertical,
                enabled = collapsableVideoState.fullscreenState.currentValue == VideoDragAnchors.Normal
            )
            .clickableNoIndication {
                if (progress < 0.1f) {
                    collapsableVideoState.expand()
                }
            }
    ) { measurables, constraints ->

        val height = lerp(CollapsablePlayerMinHeight.roundToPx(), constraints.maxHeight, progress)
        val paddingTop = (topPadding * progress).roundToInt()

        val isTablet = constraints.maxWidth > constraints.maxHeight

        val playerPlaceable = measurables
            .first { it.layoutId == "player" }
            .measure(constraints.copy(maxHeight = height - if(isTablet) lerp(0, bottomPadding, progress) else 0))

        val buttonPlaceable = measurables
            .first { it.layoutId == "scrollToTop" }
            .measure(constraints)

        val contentPlaceable = measurables
            .first { it.layoutId == "content" }
            .measure(constraints.copy(maxHeight = (height - playerPlaceable.height - paddingTop).coerceAtLeast(0)))

        val actionsPlaceable = measurables
            .first { it.layoutId == "actions" }
            .measure(constraints.copy(maxWidth = constraints.maxWidth - playerPlaceable.width))

        collapsableVideoState.fullscreenState.updateAnchors(
            DraggableAnchors {
                VideoDragAnchors.Normal at 0f
                VideoDragAnchors.FullScreen at 1200f
                VideoDragAnchors.Dismiss at 1200f + playerPlaceable.height
            }
        )

        layout(constraints.maxWidth, (height - collapsableVideoState.dismissOffsetPx).coerceAtLeast(0)) {

            val playerCenteredY = (constraints.maxHeight / 2f - playerPlaceable.height + paddingTop)
            val playerY = (playerCenteredY * collapsableVideoState.fullScreenProgress).roundToInt()

            if (isTablet) {
                val playerX = lerp(0, constraints.maxWidth / 2 - playerPlaceable.width / 2, progress)
                playerPlaceable.placeRelative(
                    playerX,
                    paddingTop + playerY.coerceAtLeast(0) + collapsableVideoState.dismissFullscreenOffsetPx
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
                    paddingTop + playerY.coerceAtLeast(0) + collapsableVideoState.dismissFullscreenOffsetPx
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
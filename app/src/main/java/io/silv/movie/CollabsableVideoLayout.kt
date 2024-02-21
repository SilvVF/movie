package io.silv.movie

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
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.silv.core_ui.components.ScrollbarLazyColumn
import io.silv.core_ui.components.clickableNoIndication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class CollapsableVideoAnchors {
    Start,
    End,
    Dismiss
}

private val LayoutMinHeight = 72.dp

@Composable
fun rememberCollapsableVideoState(): CollapsableVideoState {

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val state = remember {
        AnchoredDraggableState(
            initialValue = CollapsableVideoAnchors.Start,
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
                            with(density) { configuration.screenHeightDp.dp.toPx() + LayoutMinHeight.toPx() }
                }
            )
        }
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
        val offset = fullscreenState.requireOffset() - fullscreenState.anchors.positionOf(VideoDragAnchors.FullScreen)
        offset.coerceAtLeast(0f).roundToInt()
    }

    fun expand() {
        scope.launch {
            state.animateTo(CollapsableVideoAnchors.Start)
        }
    }
}

private class CollapsableVideoLayoutScrollConnection(
    private val lazyListState: LazyListState,
    private val state: AnchoredDraggableState<VideoDragAnchors>,
    private val scope: CoroutineScope,
): NestedScrollConnection {

    var allowedToScroll by mutableStateOf(true)

    init {
        // block nested scrolling when the user scrolls the list up
        // this prevents flinging to the start animating the player to take the screen
        scope.launch {
            snapshotFlow { lazyListState.isScrollInProgress }.collectLatest { scrolling ->
                allowedToScroll = lazyListState.firstVisibleItemIndex == 0
                while (true) {
                    if (lazyListState.firstVisibleItemIndex != 0) {
                        allowedToScroll = false
                        break
                    }
                    delay(10)
                }
            }
        }
    }

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val delta = available.y
        return if (delta < 0 && allowedToScroll) {
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
            y = if (allowedToScroll) state.dispatchRawDelta(delta) else available.y
        )
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        return if (available.y < 0 && !lazyListState.canScrollBackward && allowedToScroll) {
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
fun BoxScope.CollapsableVideoLayout(
    onDismissRequested: () -> Unit,
    player: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: LazyListScope.() -> Unit,
    modifier: Modifier = Modifier,
    collapsableVideoState: CollapsableVideoState = rememberCollapsableVideoState(),
    lazyListState: LazyListState = rememberLazyListState(),
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

    val nestedScrollConnection = remember(lazyListState, collapsableVideoState.fullscreenState) {
        CollapsableVideoLayoutScrollConnection(
            lazyListState,
            collapsableVideoState.fullscreenState,
            scope
        )
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
                    fullscreenDraggableEnabled,

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
            Box(modifier = Modifier
                .layoutId("content")
                .anchoredDraggable(
                    collapsableVideoState.fullscreenState,
                    Orientation.Vertical,
                    fullscreenDraggableEnabled
                )
            ) {
                ScrollbarLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha =
                                minOf(1f - collapsableVideoState.fullScreenProgress, contentAlpha)
                        }
                        .nestedScroll(nestedScrollConnection),
                    state = lazyListState,
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
                val visible by remember{
                    derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
                }
                AnimatedVisibility(visible = visible, enter = fadeIn(),exit = fadeOut()) {
                    Button(
                        shape = RoundedCornerShape(12),
                        modifier = Modifier.height(42.dp),
                        onClick = {
                            scope.launch { lazyListState.animateScrollToItem(0) }
                        }
                    ) {
                        Text("Scroll to top")
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
            .align(Alignment.BottomCenter)
    ) { measurables, constraints ->

        val height = lerp(LayoutMinHeight.roundToPx(), constraints.maxHeight, progress)
        val paddingTop = (topPadding * progress).roundToInt()

        val playerPlaceable = measurables
            .first { it.layoutId == "player" }
            .measure(constraints.copy(maxHeight = height))

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
package io.silv.movie

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.CoroutineScope
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

    val scope =  rememberCoroutineScope()

    return remember { CollapsableVideoState(state, scope) }
}

class CollapsableVideoState(
    val state: AnchoredDraggableState<CollapsableVideoAnchors>,
    private val scope: CoroutineScope
) {
    val progress by derivedStateOf {
        1 - (state.requireOffset() / state.anchors.positionOf(CollapsableVideoAnchors.End))
            .coerceIn(0f..1f)
    }

    fun expand() {
        scope.launch {
            state.animateTo(CollapsableVideoAnchors.Start)
        }
    }
}

private class CollapsableVideoLayoutScrollConnection(
    private val lazyListState: LazyListState,
    private val state: AnchoredDraggableState<CollapsableVideoAnchors>,
    private val enabled: Boolean,
): NestedScrollConnection {
    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val delta = available.y
        return if (delta < 0 && enabled) {
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
            y = if (enabled) state.dispatchRawDelta(delta) else available.y
        )
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        return if (available.y < 0 && !lazyListState.canScrollBackward && enabled) {
            state.animateTo(state.targetValue.takeIf { it != CollapsableVideoAnchors.Dismiss } ?: CollapsableVideoAnchors.End, available.y)
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity
    ): Velocity {
        state.animateTo(state.targetValue.takeIf { it != CollapsableVideoAnchors.Dismiss } ?: CollapsableVideoAnchors.End, available.y)
        return super.onPostFling(consumed, available)
    }
}

enum class VideoDragAnchors {
    Normal, FullScreen
}

@Composable
fun BoxScope.CollapsableVideoLayout(
    modifier: Modifier,
    onDismissRequested: () -> Unit,
    collapsableVideoState: CollapsableVideoState,
    player: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: LazyListScope.() -> Unit,
) {
    val progress = collapsableVideoState.progress
    val density= LocalDensity.current
    val state = collapsableVideoState.state
    val scope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()


    val dismissOffsetPx by remember {
        derivedStateOf {
            val offset = state.requireOffset() - state.anchors.positionOf(CollapsableVideoAnchors.End)
            offset.coerceAtLeast(0f).roundToInt()
        }
    }

    val padding = WindowInsets.systemBars.getTop(density)
    val paddingBottom = WindowInsets.systemBars.getBottom(density)

    val actionsAlpha = lerp(1f, 0f, progress / 0.1f)
    val contentAlpha = lerp(0f, 1f, progress / 0.8f)

    LaunchedEffect(Unit) {
        snapshotFlow { state.currentValue }.collect {
            if (state.currentValue == CollapsableVideoAnchors.Dismiss) {
                onDismissRequested()
            }
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
                }
            )
        }
    }

    val fullScreenProgress by remember {
        derivedStateOf {
            (fullScreenVideoDraggable.requireOffset() / fullScreenVideoDraggable.anchors.positionOf(VideoDragAnchors.FullScreen))
                .coerceIn(0f..1f)
        }
    }

    val dragEnabled by remember {
        derivedStateOf {
            fullScreenVideoDraggable.currentValue == VideoDragAnchors.Normal
        }
    }

    val nestedScrollConnection = remember(lazyListState, state, dragEnabled) {
        CollapsableVideoLayoutScrollConnection(lazyListState, state, dragEnabled)
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
            Box(modifier = Modifier
                .height(IntrinsicSize.Max)
                .aspectRatio(16f / 9f)
                .anchoredDraggable(
                    fullScreenVideoDraggable,
                    Orientation.Vertical
                )
                .layoutId("player")
            ) {
                player()
            }
            Box(modifier = Modifier.layoutId("content")) {
                ScrollbarLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = minOf(1f - fullScreenProgress, contentAlpha)
                        }
                        .nestedScroll(nestedScrollConnection),
                    state = lazyListState
                ) {
                    content()
                    item {
                        Spacer(
                            Modifier.height(
                                with(density) {
                                    paddingBottom.toDp() + 42.dp
                                }
                            )
                        )
                    }
                }
            }
            Box(modifier = Modifier
                .graphicsLayer {
                    alpha = 1f - fullScreenProgress
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
                state,
                Orientation.Vertical,
                enabled = dragEnabled
            )
            .clickable {
                if (progress < 0.1f) {
                    collapsableVideoState.expand()
                }
            }
            .align(Alignment.BottomCenter)
    ) { measurables, constraints ->

        val height = lerp(LayoutMinHeight.roundToPx(), constraints.maxHeight, progress)
        val paddingTop = (padding * progress).roundToInt()

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

        layout(constraints.maxWidth, (height - dismissOffsetPx).coerceAtLeast(0)) {

            val playerY = ((constraints.maxHeight / 2f - playerPlaceable.height + paddingTop) * fullScreenProgress).roundToInt()

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
                paddingTop + playerY.coerceAtLeast(0)
            )

            if (progress == 1f) {
                buttonPlaceable.placeRelative(
                    x = constraints.maxWidth / 2 - buttonPlaceable.width / 2,
                    y = constraints.maxHeight - buttonPlaceable.height - paddingBottom
                )
            }
        }
    }
}
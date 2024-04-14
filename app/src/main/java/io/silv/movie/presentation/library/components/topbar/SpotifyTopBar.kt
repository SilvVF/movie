package io.silv.movie.presentation.library.components.topbar

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.roundToInt

private val TopPadding = 24.dp
private val BottomPadding = 12.dp
private val TopAppBarHeight = 64.dp + TopPadding
private val TopBarMaxHeight = 482.dp
private val SearchBarHeight = 38.dp
private val TopBarPinnedHeight = 64.dp + SearchBarHeight + TopPadding

@Composable
fun rememberTopBarState(
    lazyListState: LazyListState?,
    lazyGridState: LazyGridState? = null,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): TopBarState {

    val density = LocalDensity.current
    val appBarMaxHeightPx = with(density) { TopBarMaxHeight.toPx() }
    val appBarPinnedHeightPx = with(density) { TopBarPinnedHeight.toPx() }
    val topAppBarHeightPx = with(density) { TopAppBarHeight.toPx() }
    val snapAnimationSpec = tween<Float>()
    val flingAnimationSpec = rememberSplineBasedDecay<Float>()

    return rememberSaveable(
        lazyListState, lazyGridState, coroutineScope,
        saver = Saver(
            save = { arrayOf(it.fraction, it.searching) },
            restore = { (fraction, searching ) ->
                TopBarState(
                    lazyListState,
                    lazyGridState,
                    run {
                        val res = if ((searching as? Boolean) == true) {
                            -appBarMaxHeightPx + topAppBarHeightPx
                        } else {
                            val canConsume =    if (lazyListState != null) {
                                !lazyListState.canScrollBackward ||
                                        (lazyListState.firstVisibleItemScrollOffset == 0 && lazyListState.firstVisibleItemIndex == 0)
                            } else if (lazyGridState != null) {
                                !lazyGridState.canScrollBackward ||
                                        (lazyGridState.firstVisibleItemScrollOffset == 0 && lazyGridState.firstVisibleItemIndex == 0)
                            } else {
                                false
                            }
                            if (canConsume) (-appBarMaxHeightPx + topAppBarHeightPx) * (1f - ((fraction as? Float) ?: 0f)) else  (-appBarMaxHeightPx + topAppBarHeightPx)
                        }
                        Timber.d("TopBarState", "restored: f $fraction, m $appBarMaxHeightPx, r $res, $searching")
                        res
                    },
                    searching as? Boolean == true,
                    appBarMaxHeightPx,
                    appBarPinnedHeightPx,
                    topAppBarHeightPx,
                    snapAnimationSpec,
                    flingAnimationSpec,
                    coroutineScope
                )
            }
        ),
    ) {
        TopBarState(
            lazyListState,
            lazyGridState,
            0f,
            false,
            appBarMaxHeightPx,
            appBarPinnedHeightPx,
            topAppBarHeightPx,
            snapAnimationSpec,
            flingAnimationSpec,
            coroutineScope
        )
    }
}

class TopBarState(
    private val lazyListState: LazyListState?,
    private val lazyGridState: LazyGridState?,
    initialHeight: Float,
    initialSearching: Boolean,
    maxHeightPx: Float,
    pinnedHeightPx: Float,
    topAppBarHeightPx: Float,
    snapAnimationSpec: AnimationSpec<Float>,
    flingAnimationSpec: DecayAnimationSpec<Float>,
    scope: CoroutineScope,
) {

    var searching by mutableStateOf(initialSearching)

    val connection = CollapsingAppBarNestedScrollConnection(
        initialHeight,
        maxHeightPx - topAppBarHeightPx,
        pinnedHeightPx,
        {
            if(searching) {
                return@CollapsingAppBarNestedScrollConnection false
            }

            if (lazyListState != null) {
                !lazyListState.canScrollBackward ||
                        (lazyListState.firstVisibleItemScrollOffset == 0 && lazyListState.firstVisibleItemIndex == 0)
            } else if (lazyGridState != null) {
                !lazyGridState.canScrollBackward ||
                        (lazyGridState.firstVisibleItemScrollOffset == 0 && lazyGridState.firstVisibleItemIndex == 0)
            } else {
                false
            }
        },
        snapAnimationSpec,
        flingAnimationSpec
    )

    init {
        scope.launch {
            snapshotFlow { searching }
                .drop(1)
                .collectLatest {
                    animate(
                        initialValue = connection.appBarOffset,
                        targetValue = if (searching) -maxHeightPx + topAppBarHeightPx else -pinnedHeightPx,
                        block = { value, _ ->
                            connection.appBarOffset = value
                        }
                    )
                }
        }
    }

    val fraction by derivedStateOf {
        (maxHeightPx - topAppBarHeightPx + connection.appBarOffset) / (maxHeightPx - topAppBarHeightPx)
    }

    fun requireScrollableState(): ScrollableState {
        val state: ScrollableState? = lazyListState ?: lazyGridState
        return state!!
    }

    val spaceHeightPx by derivedStateOf {
        maxHeightPx + connection.appBarOffset
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyTopBarLayout(
    topBarState: TopBarState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    search: @Composable () -> Unit = { SearchField() },
    topAppBar: @Composable () -> Unit = { TopAppBar(title = { Text("Title") })},
    poster: @Composable () -> Unit = {
        Poster(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
        )
    },
    info: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {

    BackHandler(topBarState.searching) {
        topBarState.searching = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .appBarDraggable(topBarState)
            ) {
                topAppBar()
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
            ) {
                Spacer(
                    modifier = Modifier.height(
                        with(LocalDensity.current) { topBarState.spaceHeightPx.toDp() - BottomPadding }
                    )
                )
                content(
                    PaddingValues(
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = paddingValues.calculateBottomPadding()
                    )
                )
            }

            TopBarLayout(
                topBarState,
                Modifier
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                Color.Transparent
                            )
                        )
                    ),
                TopBarPinnedHeight,
                search,
                info,
                poster,
            )
        }
    }
}

@Composable
private fun TopBarLayout(
    state: TopBarState,
    modifier: Modifier,
    pinnedHeight: Dp,
    search: @Composable () -> Unit,
    info: @Composable () -> Unit,
    poster: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .wrapContentSize()
            .appBarDraggable(topBarState = state)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .heightIn(
                    min = 0.dp,
                    max = with(LocalDensity.current) { state.spaceHeightPx.toDp() }
                )
                .fillMaxWidth(),
            color = Color.Transparent
        ) {
            Layout(
                {
                    Box(Modifier.layoutId("info")) {
                        info()
                    }
                    Box(Modifier.layoutId("search")) {
                        search()
                    }
                    Box(
                        Modifier
                            .layoutId("poster")
                            .wrapContentWidth()
                            .graphicsLayer {
                                alpha = lerp(
                                    0f,
                                    1f,
                                    FastOutLinearInEasing.transform(
                                        (state.fraction / 0.6f - 0.1f).coerceIn(0f..1f)
                                    )
                                )
                            }
                    ) {
                        poster()
                    }
                }
            ) { measurables, constraints ->
                val search = measurables.first { it.layoutId == "search" }
                val poster = measurables.first { it.layoutId == "poster" }
                val info = measurables.first { it.layoutId == "info" }

                val searchPlaceable = search.measure(
                    constraints.copy(
                        maxHeight = SearchBarHeight.roundToPx(),
                        minHeight = 0
                    )
                )
                val infoPlaceable = info.measure(constraints)
                val topPaddingPx = with(density) { TopPadding.roundToPx() }
                val bottomPaddingPx = with(density) { BottomPadding.roundToPx() }
                val posterMinHeight = with(density) { TopBarPinnedHeight.toPx() }

                val searchY =
                    TopBarPinnedHeight.toPx() + state.connection.appBarOffset - SearchBarHeight.toPx()

                val posterMaxHeight = with(density){
                    minOf(
                        (TopBarMaxHeight - TopBarPinnedHeight).toPx(),
                        (TopBarMaxHeight.toPx() + state.connection.appBarOffset),
                    )
                }

                val posterOffset = (posterMaxHeight - topPaddingPx - bottomPaddingPx - posterMinHeight - infoPlaceable.height)
                    .coerceAtMost(0f)

                val posterPlaceable = poster.measure(
                    constraints.copy(
                        minHeight = posterMinHeight.roundToInt(),
                        maxHeight = (posterMaxHeight - topPaddingPx - bottomPaddingPx - infoPlaceable.height)
                            .coerceAtLeast(posterMinHeight)
                            .roundToInt()
                    )
                )


                val posterY = (constraints.maxHeight - posterPlaceable.height - infoPlaceable.height - bottomPaddingPx)
                    .coerceAtLeast(TopPadding.roundToPx()) + posterOffset.roundToInt()



                layout(constraints.maxWidth, constraints.maxHeight) {

                    searchPlaceable.placeRelative(
                        constraints.maxWidth / 2 - searchPlaceable.width / 2,
                        searchY.roundToInt()
                    )

                    posterPlaceable.placeRelative(
                        constraints.maxWidth / 2 - posterPlaceable.width / 2,
                        maxOf(posterY, searchY.roundToInt() + searchPlaceable.height)
                    )

                    infoPlaceable.placeRelative(
                        0,
                        (constraints.maxHeight - infoPlaceable.height - bottomPaddingPx)
                    )
                }
            }
        }
    }
}


private fun Modifier.appBarDraggable(
    topBarState: TopBarState,
): Modifier = this.composed {
    this.draggable(
        enabled = !topBarState.searching,
        state = rememberDraggableState {
            if (topBarState.connection.canConsume()) {
                val newOffset = topBarState.connection.appBarOffset + it
                topBarState.connection.appBarOffset = newOffset.coerceIn(-topBarState.connection.appBarMaxHeight, 0f)
            } else {
                topBarState.requireScrollableState().dispatchRawDelta(-it)
            }
        },
        onDragStopped = { v ->
            if (topBarState.connection.canConsume()) {
                with(topBarState.connection) {
                    settleBar(
                        { appBarOffset },
                        appBarPinnedHeight,
                        appBarMaxHeight,
                        Velocity(0f, v),
                        flingAnimationSpec,
                        snapAnimationSpec,
                    ) { value ->
                        appBarOffset = value
                    }
                }
            } else {
                topBarState.requireScrollableState().scrollBy(
                    topBarState.connection.flingAnimationSpec.calculateTargetValue(0f, -v)
                )
            }
        },
        orientation = Orientation.Vertical
    )
}

@Composable
fun SearchField(modifier: Modifier = Modifier) {
    Box(modifier = modifier
        .height(42.dp)
        .fillMaxWidth()
        .background(Color.Green))
}

@Composable
fun Poster(
    modifier: Modifier
) {
    AsyncImage(
        model = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRacaxFiAzxNzKWQNWZVgfTfLXKti0MZE5HXn6_GdG0JA&s",
        contentDescription = null,
        contentScale = ContentScale.FillHeight,
        modifier = modifier
    )
}

class CollapsingAppBarNestedScrollConnection internal constructor(
    initialHeight: Float,
    val appBarMaxHeight: Float,
    val appBarPinnedHeight: Float,
    val canConsume: () -> Boolean,
    val snapAnimationSpec: AnimationSpec<Float>,
    val flingAnimationSpec: DecayAnimationSpec<Float>
) : NestedScrollConnection {

    var appBarOffset: Float by mutableFloatStateOf(initialHeight)

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.y
        return if (canConsume()) {
            val newOffset = appBarOffset + delta
            val previousOffset = appBarOffset
            appBarOffset = newOffset.coerceIn(-appBarMaxHeight, 0f)
            val consumed = appBarOffset - previousOffset
            Offset(
                x = available.x,
                y = consumed
            )
        } else {
            appBarOffset = -appBarMaxHeight
            super.onPreScroll(available, source)
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val delta = available.y
        return if (canConsume()) {
            val newOffset = appBarOffset + delta
            val previousOffset = appBarOffset
            appBarOffset = newOffset.coerceIn(-appBarMaxHeight, 0f)
            val consumed = appBarOffset - previousOffset
            Offset(
                x = available.x,
                y = consumed
            )
        } else {
            appBarOffset = -appBarMaxHeight
            super.onPostScroll(consumed, available, source)
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val superConsumed = super.onPostFling(consumed, available)
        return superConsumed + if (canConsume()) {
            settleBar(
                { appBarOffset },
                appBarPinnedHeight,
                appBarMaxHeight,
                superConsumed,
                flingAnimationSpec,
                snapAnimationSpec
            ) {
                appBarOffset = it
            }
        } else {
            appBarOffset = -appBarMaxHeight
            Velocity.Zero
        }
    }
}


private suspend fun settleBar(
    appBarOffset: () -> Float,
    appBarPinnedHeight: Float,
    appBarMaxHeight: Float,
    superConsumed: Velocity,
    flingAnimationSpec: DecayAnimationSpec<Float>,
    snapAnimationSpec: AnimationSpec<Float>,
    setHeightOffset: (Float) -> Unit,
): Velocity {
    val initial = appBarOffset()
    if (initial > -appBarMaxHeight && initial < 0) {
        var remainingVelocity = superConsumed.y
        // In case there is an initial velocity that was left after a previous user fling, animate to
        // continue the motion to expand or collapse the app bar.
        if (abs(superConsumed.y) > 1f) {
            Log.d("Animating", "Animating")
            animate(
                initial,
                flingAnimationSpec.calculateTargetValue(initial, superConsumed.y)
                    .coerceIn(-appBarMaxHeight..0f),
                block = { value, velocity ->
                    remainingVelocity = velocity
                    setHeightOffset(value)
                }
            )
        }
        if (abs(appBarOffset()) <= appBarPinnedHeight) {
            AnimationState(
                initialValue = appBarOffset(),
                initialVelocity = remainingVelocity
            )
                .animateTo(
                    if (abs(appBarOffset()) <= appBarPinnedHeight / 2) {
                        0f
                    } else {
                        -appBarPinnedHeight
                    },
                    sequentialAnimation = true,
                    animationSpec = snapAnimationSpec
                ) {
                    setHeightOffset(value)
                }
        }
        Velocity(0f, remainingVelocity)
    }
    return superConsumed
}
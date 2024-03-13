package io.silv.core_ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.silv.core_ui.util.keyboardAsState
import kotlin.math.max
import kotlin.math.roundToInt

private val PosterBarPinnedHeight = 64.0.dp
private val PosterBarMaxHeight = 364.0.dp
private val PosterBarSearchingHeight = 144.dp
private val PosterMinHeight = PosterBarMaxHeight / 3

@Composable
fun rememberPosterTopBarState(
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
): PosterTopBarState {
    val isKeyboardOpen by keyboardAsState()

    return remember(isKeyboardOpen, scrollBehavior) {
        PosterTopBarState(
            scrollBehavior,
            isKeyboardOpen
        )
    }
}

class PosterTopBarState(
    val scrollBehavior: TopAppBarScrollBehavior,
    val isKeyboardOpen: Boolean
) {

    val progress by derivedStateOf {
        scrollBehavior.state.collapsedFraction
    }
}


@Composable
fun PosterLargeTopBar(
    state: PosterTopBarState,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors2 = TopAppBarDefaults.colors2(),
    posterContent: @Composable () -> Unit = {},
    extraContent: @Composable () -> Unit = {},
) {
    TwoRowsTopAppBarPoster(
        smallTitleTextStyle = MaterialTheme.typography.titleSmall,
        smallTitle = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        windowInsets = windowInsets,
        maxHeight = PosterBarMaxHeight,
        pinnedHeight = PosterBarPinnedHeight,
        scrollBehavior = state.scrollBehavior,
        extraContent = extraContent,
        posterContent = posterContent,
        title = title,
        titleTextStyle = MaterialTheme.typography.titleLarge,
        isKeyboardOpen = state.isKeyboardOpen
    )
}
/**
 * A two-rows top app bar that is designed to be called by the Large and Medium top app bar
 * composables.
 *
 * @throws [IllegalArgumentException] if the given [maxHeight] is equal or smaller than the
 * [pinnedHeight]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TwoRowsTopAppBarPoster(
    modifier: Modifier = Modifier,
    extraContent: @Composable () -> Unit,
    posterContent: @Composable () -> Unit,
    titleTextStyle: TextStyle,
    smallTitle: @Composable () -> Unit,
    title: @Composable () -> Unit,
    smallTitleTextStyle: TextStyle,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    windowInsets: WindowInsets,
    colors: TopAppBarColors2,
    maxHeight: Dp,
    pinnedHeight: Dp,
    scrollBehavior: TopAppBarScrollBehavior?,
    isKeyboardOpen: Boolean,
) {
    if (maxHeight <= pinnedHeight) {
        throw IllegalArgumentException(
            "A TwoRowsTopAppBar max height should be greater than its pinned height"
        )
    }

    val pinnedHeightPx: Float
    val maxHeightPx: Float

    val openHeight by animateDpAsState(
        targetValue = if(!isKeyboardOpen) {
            maxHeight
        } else {
            PosterBarSearchingHeight
        },
        label = ""
    )

    LocalDensity.current.run {
        pinnedHeightPx = if (isKeyboardOpen) openHeight.toPx() else pinnedHeight.toPx()
        maxHeightPx = openHeight.toPx()
    }

    // Sets the app bar's height offset limit to hide just the bottom title area and keep top title
    // visible when collapsed.
    SideEffect {
        if (scrollBehavior?.state?.heightOffsetLimit != pinnedHeightPx - maxHeightPx) {
            scrollBehavior?.state?.heightOffsetLimit = pinnedHeightPx - maxHeightPx
        }
    }

    // Obtain the container Color from the TopAppBarColors using the `collapsedFraction`, as the
    // bottom part of this TwoRowsTopAppBar changes color at the same rate the app bar expands or
    // collapse.
    // This will potentially animate or interpolate a transition between the container color and the
    // container's scrolled color according to the app bar's scroll state.
    val colorTransitionFraction = scrollBehavior?.state?.collapsedFraction ?: 0f
    val appBarContainerColor by rememberUpdatedState(
        colors.containerColor(colorTransitionFraction)
    )

    // Wrap the given actions in a Row.
    val actionsRow = @Composable {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
    val topTitleAlpha = TopTitleAlphaEasing.transform(colorTransitionFraction)
    // Hide the top row title semantics when its alpha value goes below 0.5 threshold.
    // Hide the bottom row title semantics when the top title semantics are active.
    val hideTopRowSemantics = colorTransitionFraction < 0.5f

    // Set up support for resizing the top app bar when vertically dragging the bar itself.
    val appBarDragModifier = if (scrollBehavior != null && !scrollBehavior.isPinned && !isKeyboardOpen) {
        Modifier.draggable(
            orientation = Orientation.Vertical,
            state = rememberDraggableState { delta ->
                scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffset + delta
            },
            onDragStopped = { velocity ->
                settleAppBar(
                    scrollBehavior.state,
                    velocity,
                    scrollBehavior.flingAnimationSpec,
                    scrollBehavior.snapAnimationSpec
                )
            }
        )
    } else {
        Modifier
    }
    val progress by animateFloatAsState(
        if(isKeyboardOpen) 1f else colorTransitionFraction,
        label = "alpha-progress"
    )

    val alphaTransform = FastOutSlowInEasing.transform(progress)
    val contentTransform = FastOutSlowInEasing.transform(colorTransitionFraction)

    Column {
        Surface(
            modifier = modifier.then(appBarDragModifier),
            color = appBarContainerColor
        ) {
            Column {
                TopAppBarLayout(
                    modifier = Modifier
                        .windowInsetsPadding(windowInsets)
                        // clip after padding so we don't show the title over the inset area
                        .clipToBounds(),
                    heightPx = if (isKeyboardOpen) with(LocalDensity.current) { PosterBarPinnedHeight.toPx() } else pinnedHeightPx,
                    navigationIconContentColor =
                    colors.navigationIconContentColor,
                    titleContentColor = colors.titleContentColor,
                    actionIconContentColor =
                    colors.actionIconContentColor,
                    title = smallTitle,
                    titleTextStyle = smallTitleTextStyle,
                    titleAlpha = if (isKeyboardOpen) 1f else topTitleAlpha,
                    titleVerticalArrangement = Arrangement.Center,
                    titleHorizontalArrangement = Arrangement.Start,
                    titleBottomPadding = 0,
                    hideTitleSemantics = hideTopRowSemantics,
                    navigationIcon = navigationIcon,
                    actions = actionsRow,
                    extraContent = {}
                )
                TopAppBarLayoutNoTitle(
                    modifier = Modifier
                        // only apply the horizontal sides of the window insets padding, since the top
                        // padding will always be applied by the layout above
                        .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal))
                        .clipToBounds(),
                    heightPx = if (isKeyboardOpen) {
                        with(LocalDensity.current) { openHeight.toPx() - PosterBarPinnedHeight.toPx() }
                    } else {
                        maxHeightPx - pinnedHeightPx + (scrollBehavior?.state?.heightOffset ?: 0f)
                    },
                    navigationIconContentColor = colors.navigationIconContentColor,
                    navigationIcon = {},
                    title = title,
                    titleTextStyle = titleTextStyle,
                    actionIconContentColor = colors.actionIconContentColor,
                    extraContent = {
                        Box(
                            Modifier
                                .graphicsLayer { alpha = 1f - contentTransform }
                        ) {
                            extraContent()
                        }
                    },
                    posterContent = {
                        Box(
                            Modifier
                                .graphicsLayer { alpha = 1f - alphaTransform }
                        ) {
                            posterContent()
                        }
                    },
                    actions = {},
                    titleContentColor = colors.titleContentColor,
                    titleAlpha = 1f - alphaTransform
                )
            }
        }
    }
}

@Composable
internal fun TopAppBarLayoutNoTitle(
    modifier: Modifier,
    heightPx: Float,
    title: @Composable () -> Unit,
    titleAlpha: Float,
    titleTextStyle: TextStyle,
    navigationIconContentColor: Color,
    navigationIcon: @Composable () -> Unit,
    actionIconContentColor: Color,
    titleContentColor: Color,
    actions: @Composable () -> Unit,
    extraContent: @Composable () -> Unit,
    posterContent: @Composable () -> Unit,
) {
    Layout(
        {
            Box(
                Modifier
                    .layoutId("navigationIcon")
                    .padding(start = TopAppBarHorizontalPadding)
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides navigationIconContentColor,
                    content = navigationIcon
                )
            }
            Box(
                Modifier.layoutId("extraContent")
            ) {
                extraContent()
            }
            Box(
                Modifier.layoutId("posterContent")
            ) {
                posterContent()
            }
            Box(
                Modifier
                    .layoutId("actionIcons")
                    .padding(end = TopAppBarHorizontalPadding)
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides actionIconContentColor,
                    content = actions
                )
            }
            Box(
                Modifier
                    .layoutId("title")
                    .padding(horizontal = TopAppBarHorizontalPadding)
                    .graphicsLayer(alpha = titleAlpha)
            ) {
                ProvideTextStyle(value = titleTextStyle) {
                    CompositionLocalProvider(
                        LocalContentColor provides titleContentColor,
                        content = title
                    )
                }
            }
        },
        modifier = modifier
    ) { measurables, constraints ->

        val navigationIconPlaceable =
            measurables.first { it.layoutId == "navigationIcon" }
                .measure(constraints.copy(minWidth = 0))
        val actionIconsPlaceable =
            measurables.first { it.layoutId == "actionIcons" }
                .measure(constraints.copy(minWidth = 0))

        val extraContentPlaceable =
            measurables.first { it.layoutId == "extraContent" }
                .measure(constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxWidth = constraints.maxWidth,
                    maxHeight = PosterBarSearchingHeight.roundToPx()
                )
        )
        val layoutHeight = heightPx.roundToInt()

        val maxTitleWidth = if (constraints.maxWidth == Constraints.Infinity) {
            constraints.maxWidth
        } else {
            (constraints.maxWidth - navigationIconPlaceable.width - actionIconsPlaceable.width)
                .coerceAtLeast(0)
        }
        val titlePlaceable =
            measurables.first { it.layoutId == "title" }
                .measure(constraints.copy(minWidth = 0, maxWidth = maxTitleWidth))

        val posterContentPlaceable =
            measurables.first { it.layoutId == "posterContent" }
                .measure(constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxHeight = (layoutHeight - extraContentPlaceable.height - titlePlaceable.height)
                        .coerceAtLeast(PosterMinHeight.roundToPx())
                ))


        layout(constraints.maxWidth, layoutHeight.coerceAtLeast(0)) {
            // Navigation icon
            navigationIconPlaceable.placeRelative(
                x = 0,
                y = (layoutHeight - navigationIconPlaceable.height) / 2 - extraContentPlaceable.height
            )

            posterContentPlaceable.placeRelative(
                x = constraints.maxWidth / 2 - posterContentPlaceable.width / 2,
                y = layoutHeight - posterContentPlaceable.height - extraContentPlaceable.height - titlePlaceable.height
            )

            extraContentPlaceable.placeRelative(
                x = 0,
                y = layoutHeight - extraContentPlaceable.height
            )

            // Action icons
            actionIconsPlaceable.placeRelative(
                x = constraints.maxWidth - actionIconsPlaceable.width,
                y = (layoutHeight - actionIconsPlaceable.height) / 2  - extraContentPlaceable.height
            )

            titlePlaceable.placeRelative(
                x = max(TopAppBarTitleInset.roundToPx(), navigationIconPlaceable.width),
                y = layoutHeight - extraContentPlaceable.height - titlePlaceable.height
            )
        }
    }
}


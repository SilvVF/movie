package io.silv.core_ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.silv.core_ui.util.keyboardAsState
import kotlin.math.roundToInt

private val PosterBarPinnedHeight = 64.0.dp
private val PosterBarMaxHeight = 364.0.dp
private val PosterBarSearchingHeight = 164.0.dp
private val PosterMinHeight = PosterBarMaxHeight / 3

@Composable
fun PosterLargeTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors2 = TopAppBarDefaults.colors2(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    posterContent: @Composable (fraction: Float) -> Unit = {},
    extraContent: @Composable (fraction: Float) -> Unit = {},
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
        scrollBehavior = scrollBehavior,
        extraContent = extraContent,
        posterContent = posterContent
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
    extraContent: @Composable (progress: Float) -> Unit,
    posterContent: @Composable (progress: Float) -> Unit,
    smallTitle: @Composable () -> Unit,
    smallTitleTextStyle: TextStyle,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    windowInsets: WindowInsets,
    colors: TopAppBarColors2,
    maxHeight: Dp,
    pinnedHeight: Dp,
    scrollBehavior: TopAppBarScrollBehavior?
) {
    if (maxHeight <= pinnedHeight) {
        throw IllegalArgumentException(
            "A TwoRowsTopAppBar max height should be greater than its pinned height"
        )
    }
    val isKeyboardOpen by keyboardAsState()
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
        pinnedHeightPx = pinnedHeight.toPx()
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
                heightPx = pinnedHeightPx,
                navigationIconContentColor =
                colors.navigationIconContentColor,
                titleContentColor = colors.titleContentColor,
                actionIconContentColor =
                colors.actionIconContentColor,
                title = smallTitle,
                titleTextStyle = smallTitleTextStyle,
                titleAlpha = topTitleAlpha,
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
                heightPx = maxHeightPx - pinnedHeightPx + (scrollBehavior?.state?.heightOffset ?: 0f),
                navigationIconContentColor = colors.navigationIconContentColor,
                navigationIcon = {},
                actions = {},
                actionIconContentColor = colors.actionIconContentColor,
                extraContent = {
                    extraContent(topTitleAlpha)
                },
                posterContent = {
                    posterContent(FastOutSlowInEasing.transform(colorTransitionFraction))
                }
            )
        }
    }
}

@Composable
internal fun TopAppBarLayoutNoTitle(
    modifier: Modifier,
    heightPx: Float,
    navigationIconContentColor: Color,
    navigationIcon: @Composable () -> Unit,
    actionIconContentColor: Color,
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

        val posterContentPlaceable =
            measurables.first { it.layoutId == "posterContent" }
                .measure(constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxHeight = (layoutHeight - extraContentPlaceable.height)
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
                y = layoutHeight - posterContentPlaceable.height - extraContentPlaceable.height
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
        }
    }
}

@Composable
internal fun TopAppBarLayoutNoTitleSearching(
    modifier: Modifier,
    navigationIconContentColor: Color,
    navigationIcon: @Composable () -> Unit,
    actionIconContentColor: Color,
    actions: @Composable () -> Unit,
    extraContent: @Composable () -> Unit,
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
                Modifier
                    .layoutId("actionIcons")
                    .padding(end = TopAppBarHorizontalPadding)
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides actionIconContentColor,
                    content = actions
                )
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
                )
            )
        val layoutHeight = extraContentPlaceable.height

        layout(constraints.maxWidth, layoutHeight.coerceAtLeast(0)) {
            // Navigation icon
            navigationIconPlaceable.placeRelative(
                x = 0,
                y = (layoutHeight - navigationIconPlaceable.height) / 2 - extraContentPlaceable.height
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
        }
    }
}
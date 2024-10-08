package io.silv.core_ui.components.bottomsheet.modal

/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context
import android.graphics.Outline
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.view.WindowCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.silv.core_ui.components.bottomsheet.DraggableAnchors
import io.silv.core_ui.components.bottomsheet.draggableAnchors
import io.silv.core_ui.components.bottomsheet.modal.SheetValue.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * <a href="https://m3.material.io/components/bottom-sheets/overview" class="external" target="_blank">Material Design modal bottom sheet</a>.
 *
 * Modal bottom sheets are used as an alternative to inline menus or simple dialogs on mobile,
 * especially when offering a long list of action items, or when items require longer descriptions
 * and icons. Like dialogs, modal bottom sheets appear in front of app content, disabling all other
 * app functionality when they appear, and remaining on screen until confirmed, dismissed, or a
 * required action has been taken.
 *
 * ![Bottom sheet image](https://developer.android.com/images/reference/androidx/compose/material3/bottom_sheet.png)
 *
 * A simple example of a modal bottom sheet looks like this:
 *
 * @sample androidx.compose.material3.samples.ModalBottomSheetSample
 *
 * @param onDismissRequest Executes when the user clicks outside of the bottom sheet, after sheet
 * animates to [Hidden].
 * @param modifier Optional [Modifier] for the bottom sheet.
 * @param sheetState The state of the bottom sheet.
 * @param sheetMaxWidth [Dp] that defines what the maximum width the sheet will take.
 * Pass in [Dp.Unspecified] for a sheet that spans the entire screen width.
 * @param shape The shape of the bottom sheet.
 * @param containerColor The color used for the background of this bottom sheet
 * @param contentColor The preferred color for content inside this bottom sheet. Defaults to either
 * the matching content color for [containerColor], or to the current [LocalContentColor] if
 * [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 * overlay is applied on top of the container. A higher tonal elevation value will result in a
 * darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param scrimColor Color of the scrim that obscures content when the bottom sheet is open.
 * @param dragHandle Optional visual marker to swipe the bottom sheet.
 * @param contentWindowInsets window insets to be passed to the bottom sheet content via [PaddingValues]
 * params.
 * @param properties [ModalBottomSheetProperties] for further customization of this
 * modal bottom sheet's window behavior.
 * @param content The content to be displayed inside the bottom sheet.
 */
@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    canDrag: Boolean = true,
    sheetState: io.silv.core_ui.components.bottomsheet.modal.SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties,
    pinnedContent: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val animateToDismiss: () -> Unit = {
        if (sheetState.anchoredDraggableState.confirmValueChange(Hidden)) {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    onDismissRequest()
                }
            }
        }
    }
    val settleToDismiss: (velocity: Float) -> Unit = {
        scope.launch { sheetState.settle(it) }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismissRequest()
        }
    }

    ModalBottomSheetDialog(
        properties = properties,
        onDismissRequest = {
            if (sheetState.currentValue == Expanded && sheetState.hasPartiallyExpandedState) {
                // Smoothly animate away predictive back transformations since we are not fully
                // dismissing. We don't need to do this in the else below because we want to
                // preserve the predictive back transformations (scale) during the hide animation.
                scope.launch { sheetState.partialExpand() }
            } else { // Is expanded without collapsed state or is collapsed.
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
            }
        },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            propagateMinConstraints = false
        ) {
            Scrim(
                color = scrimColor,
                onDismiss = animateToDismiss,
                visible = sheetState.targetValue != Hidden
            )
            ModalBottomSheetContent(
                scope,
                canDrag,
                animateToDismiss,
                settleToDismiss,
                modifier,
                sheetState,
                sheetMaxWidth,
                shape,
                containerColor,
                contentColor,
                tonalElevation,
                dragHandle,
                contentWindowInsets,
                pinnedContent,
                content
            )
        }
    }
    if (sheetState.hasExpandedState) {
        LaunchedEffect(sheetState) {
            sheetState.show()
        }
    }
}

@Composable
internal fun BoxScope.ModalBottomSheetContent(
    scope: CoroutineScope,
    canDrag: Boolean,
    animateToDismiss: () -> Unit,
    settleToDismiss: (velocity: Float) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: io.silv.core_ui.components.bottomsheet.modal.SheetState,
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    pinnedContent: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .widthIn(max = sheetMaxWidth)
            .fillMaxWidth()
            .graphicsLayer {
                val sheetOffset = sheetState.anchoredDraggableState.offset
                val sheetHeight = size.height
                if (!sheetOffset.isNaN() && !sheetHeight.isNaN() && sheetHeight != 0f) {
                    transformOrigin =
                        TransformOrigin(0.5f, (sheetOffset + sheetHeight) / sheetHeight)
                }
            }
            .align(Alignment.TopCenter)
            .semantics { paneTitle = "bottomSheetPaneTitle" }
            .nestedScroll(
                remember(sheetState) {
                    ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                        sheetState = sheetState,
                        orientation = Orientation.Vertical,
                        canConsume = true,
                        onFling = settleToDismiss
                    )
                }
            )
            .draggableAnchors(
                sheetState.anchoredDraggableState,
                Orientation.Vertical
            ) { sheetSize, constraints ->
                val fullHeight = constraints.maxHeight.toFloat()
                val newAnchors = DraggableAnchors {
                    Hidden at fullHeight
                    if (sheetSize.height > (fullHeight / 3.5) &&
                        !sheetState.skipPartiallyExpanded
                    ) {
                        PartiallyExpanded at fullHeight / 3.5f
                    }
                    if (sheetSize.height != 0) {
                        Expanded at max(0f, fullHeight - sheetSize.height)
                    }
                }
                val newTarget = when (sheetState.anchoredDraggableState.targetValue) {
                    Hidden -> Hidden
                    PartiallyExpanded, Expanded -> {
                        val hasPartiallyExpandedState = newAnchors
                            .hasAnchorFor(PartiallyExpanded)
                        val newTarget = if (hasPartiallyExpandedState) PartiallyExpanded
                        else if (newAnchors.hasAnchorFor(Expanded)) Expanded else Hidden
                        newTarget
                    }
                }
                return@draggableAnchors newAnchors to newTarget
            }
            .then(
                if (canDrag) {
                    Modifier.draggable(
                        state = sheetState.anchoredDraggableState.draggableState,
                        orientation = Orientation.Vertical,
                        enabled = sheetState.isVisible,
                        startDragImmediately = sheetState.anchoredDraggableState.isAnimationRunning,
                        onDragStopped = { settleToDismiss(it) }
                    )
                } else Modifier
            ),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(contentWindowInsets())
        ) {
            if (dragHandle != null) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .semantics(mergeDescendants = true) {
                            // Provides semantics to interact with the bottomsheet based on its
                            // current value.
                            with(sheetState) {
                                dismiss("dismissActionLabel") {
                                    animateToDismiss()
                                    true
                                }
                                if (currentValue == PartiallyExpanded) {
                                    expand("expandActionLabel") {
                                        if (anchoredDraggableState.confirmValueChange(
                                                Expanded
                                            )
                                        ) {
                                            scope.launch { sheetState.expand() }
                                        }
                                        true
                                    }
                                } else if (hasPartiallyExpandedState) {
                                    collapse("collapseActionLabel") {
                                        if (anchoredDraggableState.confirmValueChange(
                                                PartiallyExpanded
                                            )
                                        ) {
                                            scope.launch { partialExpand() }
                                        }
                                        true
                                    }
                                }
                            }
                        }.draggable(
                            state = sheetState.anchoredDraggableState.draggableState,
                            orientation = Orientation.Vertical,
                            enabled = sheetState.isVisible,
                            startDragImmediately = sheetState.anchoredDraggableState.isAnimationRunning,
                            onDragStopped = { settleToDismiss(it) }
                        )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        dragHandle()
                        HorizontalDivider()
                    }
                }
            }
            Layout(
                {
                    pinnedContent()
                    Column(content = content)
                },
                Modifier.imePadding()
            ) { measurables, constraints ->

                val sheet = measurables[1]
                val offset = sheetState.anchoredDraggableState.offset.takeIf { !it.isNaN() }?.roundToInt() ?: 0


                val pinned = measurables[0]

                val minHeight =  pinned.minIntrinsicHeight(constraints.maxWidth)

                val pinnedPlaceabled = pinned.measure(
                    constraints.copy(
                        minHeight =  minHeight,
                        maxHeight = maxOf(
                            minHeight,
                            constraints.maxHeight.coerceAtMost(constraints.maxHeight - offset)
                        )
                    )
                )

                val sheetPlaceable = sheet.measure(constraints.copy(
                    minHeight = 0,
                    maxHeight = (constraints.maxHeight - pinnedPlaceabled.height).coerceAtLeast(0)
                ))

                layout(constraints.maxWidth, constraints.maxHeight) {
                    sheetPlaceable.place(0, constraints.maxHeight - pinnedPlaceabled.height - sheetPlaceable.height)
                    pinnedPlaceabled.place(
                        0,
                        (constraints.maxHeight - pinnedPlaceabled.height - offset)
                            .coerceAtLeast(0)
                    )
                }
            }
        }
    }
}

/**
 * Default values for [ModalBottomSheet]
 */
@Immutable
object ModalBottomSheetDefaults {

    /**
     * Properties used to customize the behavior of a [ModalBottomSheet]. */
    val properties = ModalBottomSheetProperties(
        securePolicy = SecureFlagPolicy.Inherit,
        shouldDismissOnBackPress = true
    )
}

/**
 * Create and [remember] a [SheetState] for [ModalBottomSheet].
 *
 * @param skipPartiallyExpanded Whether the partially expanded state, if the sheet is tall enough,
 * should be skipped. If true, the sheet will always expand to the [Expanded] state and move to the
 * [Hidden] state when hiding the sheet, either programmatically or by user interaction.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
fun rememberModalBottomSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
) = rememberSheetState(
    skipPartiallyExpanded = skipPartiallyExpanded,
    confirmValueChange = confirmValueChange,
    initialValue = Hidden,
)


@Composable
private fun Scrim(color: Color, onDismiss: () -> Unit, visible: Boolean) {
    if (color.isSpecified) {
        val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = TweenSpec())
        val dismissModifier =
            if (visible) {
                Modifier.pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
                    .semantics(mergeDescendants = true) {
                        onClick {
                            onDismiss()
                            true
                        }
                    }
            } else {
                Modifier
            }

        Canvas(Modifier.fillMaxSize().then(dismissModifier)) {
            drawRect(color = color, alpha = alpha.coerceIn(0f, 1f))
        }
    }
}

// Fork of androidx.compose.ui.window.AndroidDialog_androidKt.Dialog
// Added predictiveBackProgress param to pass into BottomSheetDialogWrapper.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModalBottomSheetDialog(
    onDismissRequest: () -> Unit,
    properties: ModalBottomSheetProperties,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val composition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val dialogId = rememberSaveable { UUID.randomUUID() }
    val darkThemeEnabled = isSystemInDarkTheme()

    val dialog =
        remember(view, density) {
            ModalBottomSheetDialogWrapper(
                onDismissRequest,
                properties,
                view,
                layoutDirection,
                density,
                dialogId,
                darkThemeEnabled,
            )
                .apply {
                    setContent(composition) {
                        Box(
                            Modifier.semantics { dialog() },
                        ) {
                            currentContent()
                        }
                    }
                }
        }

    DisposableEffect(dialog) {
        dialog.show()

        onDispose {
            dialog.dismiss()
            dialog.disposeComposition()
        }
    }

    SideEffect {
        dialog.updateParameters(
            onDismissRequest = onDismissRequest,
            properties = properties,
            layoutDirection = layoutDirection
        )
    }
}

// Fork of androidx.compose.ui.window.DialogLayout
// Additional parameters required for current predictive back implementation.
@Suppress("ViewConstructor")
private class ModalBottomSheetDialogLayout(
    context: Context,
    override val window: Window,
) : AbstractComposeView(context), DialogWindowProvider {

    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
        createComposition()
    }

    // Display width and height logic removed, size will always span fillMaxSize().

    @Composable
    override fun Content() {
        content()
    }
}
private val PredictiveBackEasing: Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

object PredictiveBack {
    fun transform(progress: Float) = PredictiveBackEasing.transform(progress)
}

// Fork of androidx.compose.ui.window.DialogWrapper.
// predictiveBackProgress and scope params added for predictive back implementation.
// EdgeToEdgeFloatingDialogWindowTheme provided to allow theme to extend into status bar.
@ExperimentalMaterial3Api
private class ModalBottomSheetDialogWrapper(
    private var onDismissRequest: () -> Unit,
    private var properties: ModalBottomSheetProperties,
    private val composeView: View,
    layoutDirection: LayoutDirection,
    density: Density,
    dialogId: UUID,
    darkThemeEnabled: Boolean,
) :
    ComponentDialog(
        ContextThemeWrapper(
            composeView.context,
            androidx.compose.material3.R.style.EdgeToEdgeFloatingDialogWindowTheme
        )
    ),
    ViewRootForInspector {

    private val dialogLayout: ModalBottomSheetDialogLayout

    // On systems older than Android S, there is a bug in the surface insets matrix math used by
    // elevation, so high values of maxSupportedElevation break accessibility services: b/232788477.
    private val maxSupportedElevation = 8.dp

    override val subCompositionView: AbstractComposeView
        get() = dialogLayout

    init {
        val window = window ?: error("Dialog has no window")
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        dialogLayout =
            ModalBottomSheetDialogLayout(
                context,
                window
            )
                .apply {
                    // Set unique id for AbstractComposeView. This allows state restoration for the
                    // state
                    // defined inside the Dialog via rememberSaveable()
                    setTag(R.id.compose_view_saveable_id_tag, "Dialog:$dialogId")
                    // Enable children to draw their shadow by not clipping them
                    clipChildren = false
                    // Allocate space for elevation
                    with(density) { elevation = maxSupportedElevation.toPx() }
                    // Simple outline to force window manager to allocate space for shadow.
                    // Note that the outline affects clickable area for the dismiss listener. In
                    // case of
                    // shapes like circle the area for dismiss might be to small (rectangular
                    // outline
                    // consuming clicks outside of the circle).
                    outlineProvider =
                        object : ViewOutlineProvider() {
                            override fun getOutline(view: View, result: Outline) {
                                result.setRect(0, 0, view.width, view.height)
                                // We set alpha to 0 to hide the view's shadow and let the
                                // composable to draw
                                // its own shadow. This still enables us to get the extra space
                                // needed in the
                                // surface.
                                result.alpha = 0f
                            }
                        }
                }
        // Clipping logic removed because we are spanning edge to edge.

        setContentView(dialogLayout)
        dialogLayout.setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        dialogLayout.setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        dialogLayout.setViewTreeSavedStateRegistryOwner(
            composeView.findViewTreeSavedStateRegistryOwner()
        )

        // Initial setup
        updateParameters(onDismissRequest, properties, layoutDirection)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkThemeEnabled
            isAppearanceLightNavigationBars = !darkThemeEnabled
        }
        // Due to how the onDismissRequest callback works
        // (it enforces a just-in-time decision on whether to update the state to hide the dialog)
        // we need to unconditionally add a callback here that is always enabled,
        // meaning we'll never get a system UI controlled predictive back animation
        // for these dialogs
        onBackPressedDispatcher.addCallback(this) {
            if (properties.shouldDismissOnBackPress) {
                onDismissRequest()
            }
        }
    }

    private fun setLayoutDirection(layoutDirection: LayoutDirection) {
        dialogLayout.layoutDirection =
            when (layoutDirection) {
                LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
                LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
            }
    }

    fun setContent(parentComposition: CompositionContext, children: @Composable () -> Unit) {
        dialogLayout.setContent(parentComposition, children)
    }

    private fun setSecurePolicy(securePolicy: SecureFlagPolicy) {
        val secureFlagEnabled =
            securePolicy.shouldApplySecureFlag(composeView.isFlagSecureEnabled())
        window!!.setFlags(
            if (secureFlagEnabled) {
                WindowManager.LayoutParams.FLAG_SECURE
            } else {
                WindowManager.LayoutParams.FLAG_SECURE.inv()
            },
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    fun updateParameters(
        onDismissRequest: () -> Unit,
        properties: ModalBottomSheetProperties,
        layoutDirection: LayoutDirection
    ) {
        this.onDismissRequest = onDismissRequest
        this.properties = properties
        setSecurePolicy(properties.securePolicy)
        setLayoutDirection(layoutDirection)

        // Window flags to span parent window.
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )
        window?.setSoftInputMode(
            if (Build.VERSION.SDK_INT >= 30) {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            } else {
                @Suppress("DEPRECATION") WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            },
        )
    }

    fun disposeComposition() {
        dialogLayout.disposeComposition()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = super.onTouchEvent(event)
        if (result) {
            onDismissRequest()
        }

        return result
    }

    override fun cancel() {
        // Prevents the dialog from dismissing itself
        return
    }
}

internal fun View.isFlagSecureEnabled(): Boolean {
    val windowParams = rootView.layoutParams as? WindowManager.LayoutParams
    if (windowParams != null) {
        return (windowParams.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
    }
    return false
}

/** Policy on setting [WindowManager.LayoutParams.FLAG_SECURE] on a window. */
enum class SecureFlagPolicy {
    /**
     * Inherit [WindowManager.LayoutParams.FLAG_SECURE] from the parent window and pass it on the
     * window that is using this policy.
     */
    Inherit,

    /**
     * Forces [WindowManager.LayoutParams.FLAG_SECURE] to be set on the window that is using this
     * policy.
     */
    SecureOn,
    /**
     * No [WindowManager.LayoutParams.FLAG_SECURE] will be set on the window that is using this
     * policy.
     */
    SecureOff
}

internal fun SecureFlagPolicy.shouldApplySecureFlag(isSecureFlagSetOnParent: Boolean): Boolean {
    return when (this) {
        SecureFlagPolicy.SecureOff -> false
        SecureFlagPolicy.SecureOn -> true
        SecureFlagPolicy.Inherit -> isSecureFlagSetOnParent
    }
}

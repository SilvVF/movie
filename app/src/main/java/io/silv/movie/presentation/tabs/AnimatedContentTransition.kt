package io.silv.movie.presentation.tabs

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import io.silv.core_ui.components.PosterData
import io.silv.core_ui.components.bottomsheet.modal.PredictiveBack
import io.silv.movie.presentation.LocalAppData
import io.silv.movie.presentation.LocalVideoState
import io.silv.movie.presentation.media.components.CollapsableVideoAnchors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

@Composable
fun SharedElementTransitionTab.AnimatedContentTransition(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    transform: AnimatedContentTransitionScope<Screen>.() -> ContentTransform = {
        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
            .togetherWith(fadeOut(animationSpec = tween(90)))
    },
    label: String = "AnimatedContentTransition",
    content: @Composable AnimatedVisibilityScope.(Screen) -> Unit = { it.Content() }
) {
    if (Build.VERSION.SDK_INT >= 34 && LocalAppData.current.predictiveBackNavigation) {
        PredictiveBackAnimatedContentTransition(
            navigator = navigator,
            transform = transform,
            label = label,
            modifier = modifier,
            content = content
        )
    } else {
        AnimatedContentTransitionNoPredictiveBack(
            navigator = navigator,
            transform = transform,
            label = label,
            modifier = modifier,
            content = content
        )
    }
}

@Composable
fun SharedElementTransitionTab.AnimatedContentTransitionNoPredictiveBack(
    navigator: Navigator,
    transform: AnimatedContentTransitionScope<Screen>.() -> ContentTransform,
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(Screen) -> Unit
) {
    val sharedElementTransitions = LocalAppData.current.sharedElementTransitions

    SharedTransitionScope {
        AnimatedContent(
            targetState = navigator.lastItem,
            transitionSpec = transform,
            modifier = modifier.then(it),
            label = label,
        ) { screen ->
            CompositionLocalProvider(
                LocalSharedTransitionState.provides(
                    SharedTransitionState(
                        transitionScope = this@SharedTransitionScope,
                        visibilityScope = this@AnimatedContent,
                        transform = this@AnimatedContentTransitionNoPredictiveBack.transform
                    )
                        .takeIf { sharedElementTransitions }
                )
            ) {
                navigator.saveableState(screen.key, screen) {
                    this@AnimatedContent.content(screen)
                }
            }
        }
    }
}

@Composable
fun SharedElementTransitionTab.PredictiveBackAnimatedContentTransition(
    navigator: Navigator,
    transform: AnimatedContentTransitionScope<Screen>.() -> ContentTransform,
    label: String,
    modifier: Modifier,
    content: @Composable AnimatedVisibilityScope.(Screen) -> Unit
) {
    var transitionProgress by rememberSaveable { mutableFloatStateOf(0f) }
    val videoState = LocalVideoState.current

    PredictiveBackHandler(
        enabled = navigator.items.size >= 2
    ) { progress: Flow<BackEventCompat> ->
        try {
            var shouldPop = true
            progress.collect { backevent ->
                with(videoState) {
                    if (videoDragState.currentValue == CollapsableVideoAnchors.Start) {
                        shouldPop = false
                        predictiveBack(backevent.progress)
                        return@collect
                    }
                }
                transitionProgress = PredictiveBack.transform(backevent.progress)
            }
            transitionProgress = 0f
            if (shouldPop) {
                navigator.pop()
            } else {
                videoState.dismiss()
            }
        } catch (e: CancellationException) {
            videoState.settle()
            animate(
                transitionProgress,
                targetValue = 0f,
                initialVelocity = 0f,
            ) { value, _ ->
                transitionProgress = value
            }
            transitionProgress = 0f
        }
    }

    Box {
        if (transitionProgress > 0) {
            val prevScreen = navigator.items.getOrNull(navigator.items.lastIndex - 1)
            prevScreen?.Content()
        }

        SharedTransitionScope {
            AnimatedContent(
                targetState = navigator.lastItem,
                transitionSpec = transform,
                modifier = modifier
                    .then(it)
                    .graphicsLayer {
                        val progress = transitionProgress
                        scaleX = calculatePredictiveBackScaleX(progress)
                        scaleY = calculatePredictiveBackScaleY(progress)

                        // Translation calculated based on size and progress
                        translationX = calculatePredictiveBackTranslationX(progress, size.width)
                        translationY = calculatePredictiveBackTranslationY(progress, size.height)

                        // Apply rounded corners as the user swipes back
                        clip = true
                        shape = RoundedCornerShape(12.dp * progress)
                    },
                label = label,
            ) { screen ->
                CompositionLocalProvider(
                    LocalSharedTransitionState.provides(
                        SharedTransitionState(
                            transitionScope = this@SharedTransitionScope,
                            visibilityScope = this@AnimatedContent,
                            transform = this@PredictiveBackAnimatedContentTransition.transform
                        )
                            .takeIf { LocalAppData.current.sharedElementTransitions }
                    )
                ) {
                    navigator.saveableState(screen.key, screen) {
                        content(screen)
                    }
                }
            }
        }
    }
}

private fun GraphicsLayerScope.calculatePredictiveBackScaleX(progress: Float): Float {
    // Min scale is 0.98f or you can adjust based on your preference.
    val minScale = 0.98f
    return lerp(1f, minScale, progress)
}

private fun GraphicsLayerScope.calculatePredictiveBackScaleY(progress: Float): Float {
    // Min scale is 0.98f or you can adjust based on your preference.
    val minScale = 0.98f
    return lerp(1f, minScale, progress)
}

private fun GraphicsLayerScope.calculatePredictiveBackTranslationX(
    progress: Float,
    size: Float
): Float {
    // Here, you move the view horizontally based on the view width.
    // Translation is typically a fraction of the view width.
    val maxTranslationX = (size / 20).coerceAtLeast(8.dp.toPx())
    return lerp(0f, maxTranslationX, progress)
}

private fun GraphicsLayerScope.calculatePredictiveBackTranslationY(
    progress: Float,
    size: Float
): Float {
    // Vertical translation is often less noticeable.
    val maxTranslationY = size / 20 // This can be customized.
    return lerp(0f, maxTranslationY, progress)
}

sealed class SharedElement(val key: String) {

    data class Movie(val id: Long) : SharedElement(key = PREFIX_MOVIE_POSTER + id)
    data class Show(val id: Long) : SharedElement(key = PREFIX_SHOW_POSTER + id)
    data class List(val id: Long) : SharedElement(key = PREFIX_LIST_POSTER + id)
    data class From(val id: String) : SharedElement(key = id)

    companion object {
        const val PREFIX_LIST_NAME = "list_name:"
        const val PREFIX_LIST_POSTER = "list_poster:"
        const val PREFIX_MOVIE_POSTER = "movie_poster:"
        const val PREFIX_SHOW_POSTER = "show_poster:"

        const val KEY_LIBRARY_POSTER = "library_poster"
    }
}

fun PosterData.toSharedElement() = if (isMovie) SharedElement.Movie(id) else SharedElement.Show(id)

@Composable
fun Modifier.registerSharedElement(
    element: SharedElement,
    inOverlay: Boolean = true,
    transitionState: SharedTransitionState? = (LocalTabNavigator.current.current as? SharedElementTransitionTab)
        ?.LocalSharedTransitionState
        ?.current

): Modifier = this then when (transitionState) {
    null -> Modifier
    else -> with(transitionState.transitionScope) {
        Modifier.sharedElement(
            state = rememberSharedContentState(key = element.key),
            animatedVisibilityScope = transitionState.visibilityScope,
            boundsTransform = transitionState.transform,
            renderInOverlayDuringTransition = inOverlay
        )
    }
}

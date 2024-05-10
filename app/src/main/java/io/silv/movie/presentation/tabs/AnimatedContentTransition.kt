package io.silv.movie.presentation.tabs

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import io.silv.core_ui.components.PosterData
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.presentation.LocalAppState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

private val PredictiveBackEasing: Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

private object PredictiveBack {
    fun transform(progress: Float) = PredictiveBackEasing.transform(progress)
}


private fun GraphicsLayerScope.calculatePredictiveBackScaleX(progress: Float): Float {
    val width = size.width
    return if (width.isNaN() || width == 0f) {
        1f
    } else {
        1f - lerp(0f, 0.1f, progress)
    }
}

private fun GraphicsLayerScope.calculatePredictiveBackScaleY(progress: Float): Float {
    val height = size.height
    return if (height.isNaN() || height == 0f) {
        1f
    } else {
        1f - lerp(0f, 0.1f, progress)
    }
}



context(SharedTransitionTab)
@Composable
fun AnimatedContentTransition(
    navigator: Navigator,
    transform: AnimatedContentTransitionScope<Screen>.() -> ContentTransform = {
        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
            .togetherWith(fadeOut(animationSpec = tween(90)))
    },
    label: String = "Crossfade",
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(Screen) -> Unit = { it.Content() }
) {
    val sharedElementTransitions = LocalAppState.current.sharedElementTransitions

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
                        transform =  this@SharedTransitionTab.transform
                    )
                        .takeIf { sharedElementTransitions }
                )
            ) {
                navigator.saveableState("transition", screen) {
                    this@AnimatedContent.content(screen)
                }
            }
        }
    }
}

context(SharedTransitionTab)
@Composable
fun PredictiveBackAnimatedContentTransition(
    navigator: Navigator,
    transform: AnimatedContentTransitionScope<Screen>.() -> ContentTransform = {
        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
            .togetherWith(fadeOut(animationSpec = tween(90)))
    },
    label: String = "Crossfade",
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(Screen) -> Unit = { it.Content() }
) {
    var transitionProgress by remember { mutableFloatStateOf(0f) }
    val sharedElementTransitions = LocalAppState.current.sharedElementTransitions

    PredictiveBackHandler(
        enabled = navigator.items.size >= 2
    ) { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { backevent ->
                transitionProgress = PredictiveBack.transform(backevent.progress)
            }
            transitionProgress = 0f
            navigator.pop()
        } catch (e: CancellationException) {
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
            navigator.items.getOrNull(navigator.items.lastIndex - 1)?.Content()
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

                        
                        translationX = lerp(0f, (size.width / 20).coerceAtLeast(8.dp.toPx()), progress)
                        translationY = lerp(0f, (size.height / 20), progress)
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
                            transform =  this@SharedTransitionTab.transform
                        )
                            .takeIf { sharedElementTransitions }
                    )
                ) {
                    navigator.saveableState("transition", screen) {
                        this@AnimatedContent.content(screen)
                    }
                }
            }
        }
    }
}



fun Modifier.listNameSharedElement(listId: Long, inOverlay: Boolean = true): Modifier = this.composed {

    val transitionState =
        (LocalTabNavigator.current.current as? SharedTransitionTab)
            ?.LocalSharedTransitionState
            ?.current

    this.then(
        when(transitionState) {
            null -> Modifier
            else -> {
                with(transitionState.transitionScope) {
                    Modifier.sharedElement(
                        state = rememberSharedContentState(key = "listName-$listId"),
                        animatedVisibilityScope = transitionState.visibilityScope,
                        boundsTransform = transitionState.transform,
                        renderInOverlayDuringTransition = inOverlay
                    )
                }
            }
        }
    )
}

fun Modifier.movieSharedElement(movieId: Long): Modifier = this.composed {

    val transitionState =
        (LocalTabNavigator.current.current as? SharedTransitionTab)
            ?.LocalSharedTransitionState
            ?.current

    this.then(
        when(transitionState) {
            null -> Modifier
            else -> {
                with(transitionState.transitionScope) {
                    Modifier.sharedElement(
                        state = rememberSharedContentState(key = "movie-$movieId"),
                        animatedVisibilityScope = transitionState.visibilityScope,
                        boundsTransform = transitionState.transform,
                    )
                }
            }
        }
    )
}

fun Modifier.showSharedElement(showId: Long): Modifier = this.composed {

    val transitionState =
        (LocalTabNavigator.current.current as? SharedTransitionTab)
            ?.LocalSharedTransitionState
            ?.current

    this.then(
        when(transitionState) {
            null -> Modifier
            else -> {
                with(transitionState.transitionScope) {
                    Modifier.sharedElement(
                        state = rememberSharedContentState(key = "show-$showId"),
                        animatedVisibilityScope = transitionState.visibilityScope,
                        boundsTransform = transitionState.transform,
                    )
                }
            }
        }
    )
}

fun Modifier.contentItemSharedElement(contentItem: ContentItem): Modifier = this.composed {

    val transitionState =
        (LocalTabNavigator.current.current as? SharedTransitionTab)
            ?.LocalSharedTransitionState
            ?.current


    this.then(
        when(transitionState) {
            null -> Modifier
            else -> {
                with(transitionState.transitionScope) {
                    val tag = remember(contentItem) {
                        val prefix = if (contentItem.isMovie) "movie" else "show"
                        "$prefix-${contentItem.contentId}"
                    }
                    Modifier.sharedElement(
                        state = rememberSharedContentState(key = tag),
                        animatedVisibilityScope = transitionState.visibilityScope,
                        boundsTransform = transitionState.transform,
                    )
                }
            }
        }
    )
}

fun Modifier.coverDataSharedElement(coverData: PosterData): Modifier = this.composed {

    val transitionState =
        (LocalTabNavigator.current.current as? SharedTransitionTab)
            ?.LocalSharedTransitionState
            ?.current

    this.then(
        when(transitionState) {
            null -> Modifier
            else -> {
                with(transitionState.transitionScope) {
                    val tag = remember(coverData) {
                        val prefix = if (coverData.isMovie) "movie" else "show"
                        "$prefix-${coverData.id}"
                    }
                    Modifier.sharedElement(
                        state = rememberSharedContentState(key = tag),
                        animatedVisibilityScope = transitionState.visibilityScope,
                        boundsTransform = transitionState.transform,
                    )
                }
            }
        }
    )
}

fun Modifier.posterSharedElement(
    listId: Long,
    inOverlay: Boolean = true
): Modifier = this.composed {

    val transitionState =
        (LocalTabNavigator.current.current as? SharedTransitionTab)
            ?.LocalSharedTransitionState
            ?.current


    this.then(
        when(transitionState) {
            null -> Modifier
            else -> {
                with(transitionState.transitionScope) {
                    Modifier.sharedElement(
                        state = rememberSharedContentState(key = "poster-$listId"),
                        animatedVisibilityScope = transitionState.visibilityScope,
                        renderInOverlayDuringTransition = inOverlay,
                        boundsTransform = transitionState.transform,
                    )
                }
            }
        }
    )
}
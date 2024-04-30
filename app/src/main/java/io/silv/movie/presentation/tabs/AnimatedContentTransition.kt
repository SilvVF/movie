package io.silv.movie.presentation.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import io.silv.core_ui.components.PosterData
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.presentation.LocalAppState

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
            label = label
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
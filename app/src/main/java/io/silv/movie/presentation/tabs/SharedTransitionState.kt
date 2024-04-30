package io.silv.movie.presentation.tabs

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Stable

@Stable
data class SharedTransitionState(
    val transitionScope: SharedTransitionScope,
    val visibilityScope: AnimatedVisibilityScope,
    val transform: BoundsTransform,
)
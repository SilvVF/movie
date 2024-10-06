package io.silv.movie.presentation.tabs

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import cafe.adriel.voyager.navigator.tab.Tab

abstract class SharedTransitionTab : Tab {

    val transform = defaultTransform

    val LocalSharedTransitionState = LocalTransitionState

    companion object {
        @Transient
        private val defaultTransform: BoundsTransform = BoundsTransform { _, _ ->
            tween(450, easing = EaseOutCubic)
        }

        @Transient
        private val LocalTransitionState: ProvidableCompositionLocal<SharedTransitionState?> = compositionLocalOf { null }
    }
}




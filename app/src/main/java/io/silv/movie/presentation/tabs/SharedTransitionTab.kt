package io.silv.movie.presentation.tabs

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import cafe.adriel.voyager.navigator.tab.Tab
import kotlinx.parcelize.IgnoredOnParcel

abstract class SharedTransitionTab : Tab {

    @Transient
    @IgnoredOnParcel
    val transform = defaultTransform

    @Transient
    @IgnoredOnParcel
    val LocalSharedTransitionState = LocalTransitionState

    companion object {
        @Transient
        @IgnoredOnParcel
        private val defaultTransform: BoundsTransform = BoundsTransform { _, _ ->
            tween(450, easing = EaseOutCubic)
        }

        @Transient
        @IgnoredOnParcel
        private val LocalTransitionState: ProvidableCompositionLocal<SharedTransitionState?> = compositionLocalOf { null }
    }
}




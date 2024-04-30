package io.silv.movie.presentation.tabs

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import cafe.adriel.voyager.navigator.tab.Tab

abstract class SharedTransitionTab : Tab {

    @Transient
    open val transform: BoundsTransform = BoundsTransform { _, _ ->
        tween(450, easing = EaseOutCubic)
    }

    @Transient
    val LocalSharedTransitionState: ProvidableCompositionLocal<SharedTransitionState?> =
        compositionLocalOf { null }


}




package io.silv.movie.presentation.tabs

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import io.silv.core_ui.voyager.ScreenResultsViewModel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.parcelize.IgnoredOnParcel
import org.koin.androidx.compose.koinViewModel

abstract class SharedElementTransitionTab: Tab {

    @Transient
    @IgnoredOnParcel
    val transform = defaultTransform

    @Transient
    @IgnoredOnParcel
    val LocalSharedTransitionState = LocalTransitionState

    companion object {
        private val defaultTransform: BoundsTransform = BoundsTransform { _, _ ->
            tween(450, easing = EaseOutCubic)
        }
        private val LocalTransitionState: ProvidableCompositionLocal<SharedTransitionState?> =
            compositionLocalOf { null }
    }
}


@Composable
fun Navigator.ConsumeNavEvents() {

    val screenResultsViewModel = koinViewModel<ScreenResultsViewModel>()
    val lifecycle = LocalLifecycleOwner.current

    LaunchedEffect(lifecycle, screenResultsViewModel) {
        lifecycle.repeatOnLifecycle(State.STARTED) {
            screenResultsViewModel.screenNavCh.receiveAsFlow().collect { navEvent ->
                navEvent()
            }
        }
    }
}




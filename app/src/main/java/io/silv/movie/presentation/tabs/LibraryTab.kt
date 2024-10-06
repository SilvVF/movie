package io.silv.movie.presentation.tabs

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.silv.movie.MainScreenModel
import io.silv.movie.R
import io.silv.movie.presentation.getActivityViewModel
import io.silv.movie.presentation.screen.LibraryScreen
import kotlinx.coroutines.flow.receiveAsFlow


data object LibraryTab: SharedTransitionTab() {

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = stringResource(id = R.string.library_tab_title),
            icon = rememberVectorPainter(image = Icons.AutoMirrored.Rounded.LibraryBooks)
        )

    @Composable
    override fun Content() {

        val mainScreenModel by getActivityViewModel<MainScreenModel>()

        Navigator(
            LibraryScreen
        ) { navigator ->

            LaunchedEffect(Unit) {
                mainScreenModel.navigationChannel.receiveAsFlow().collect { action ->
                    with(navigator) { action() }
                }
            }

            AnimatedContentTransition(
                navigator,
                transform = {
                    fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                }
            )
        }
    }
}







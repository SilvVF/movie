package io.silv.movie.presentation.library

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.silv.movie.Nav
import io.silv.movie.R
import io.silv.movie.presentation.AnimatedContentTransition
import io.silv.movie.presentation.SharedTransitionTab
import io.silv.movie.presentation.library.screens.LibraryScreen


data object LibraryTab: SharedTransitionTab() {

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = stringResource(id = R.string.library_tab_title),
            icon = rememberVectorPainter(image = Icons.AutoMirrored.Rounded.LibraryBooks)
        )

    @Composable
    override fun Content() {
        Navigator(
            LibraryScreen
        ) { navigator ->

            SideEffect { Nav.setNav(navigator) }

            AnimatedContentTransition(
                navigator,
                transform = {
                    fadeIn() togetherWith fadeOut()
                }
            )
        }
    }
}







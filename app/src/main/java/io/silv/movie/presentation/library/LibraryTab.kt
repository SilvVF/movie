package io.silv.movie.presentation.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.movie.LocalGlobalNavigator
import io.silv.movie.R
import io.silv.movie.presentation.library.screens.LibraryScreen


object LibraryTab: Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = stringResource(id = R.string.library_tab_title),
            icon = rememberVectorPainter(image = Icons.AutoMirrored.Rounded.LibraryBooks)
        )

    @Composable
    override fun Content() {

        val currentNav = LocalGlobalNavigator.current

        Navigator(
            LibraryScreen()
        ) {

            SideEffect {
                currentNav.value = it
            }

            FadeTransition(it)
        }
    }
}






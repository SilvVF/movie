package io.silv.movie.presentation.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.movie.Nav
import io.silv.movie.R
import io.silv.movie.presentation.settings.LocalBackPress
import io.silv.movie.presentation.settings.SettingsMainScreen

data object SettingsTab: Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 2u,
            title = stringResource(id = R.string.settings_tab_title),
            icon = rememberVectorPainter(image = Icons.Filled.Settings)
        )

    @Composable
    override fun Content() {
        val tabNavigator = LocalTabNavigator.current
        Navigator(SettingsMainScreen) { navigator ->
            val pop: () -> Unit = {
                if (navigator.canPop) {
                    navigator.pop()
                } else {
                    tabNavigator.current = LibraryTab
                }
            }
            CompositionLocalProvider(LocalBackPress provides pop) {
                SideEffect { Nav.setNav(navigator) }

                FadeTransition(navigator)
            }
        }
    }
}
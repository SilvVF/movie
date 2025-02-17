package io.silv.movie.presentation.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.silv.movie.MainViewModel
import io.silv.movie.R
import io.silv.movie.presentation.getActivityViewModel
import io.silv.movie.presentation.settings.LocalBackPress
import io.silv.movie.presentation.settings.SettingsMainScreen
import kotlinx.coroutines.flow.receiveAsFlow

data object SettingsTab: SharedTransitionTab() {

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 2u,
            title = stringResource(id = R.string.settings_tab_title),
            icon = rememberVectorPainter(image = Icons.Filled.Settings)
        )

    @Composable
    override fun Content() {
        val tabNavigator = LocalTabNavigator.current
        val mainViewModel by getActivityViewModel<MainViewModel>()

        Navigator(SettingsMainScreen) { navigator ->
            val pop: () -> Unit = {
                if (navigator.canPop) {
                    navigator.pop()
                } else {
                    tabNavigator.current = LibraryTab
                }
            }

            LaunchedEffect(Unit) {
                mainViewModel.navigationChannel.receiveAsFlow().collect { action ->
                    with(navigator) { action() }
                }
            }

            CompositionLocalProvider(LocalBackPress provides pop) {
                AnimatedContentTransition(navigator)
            }
        }
    }
}
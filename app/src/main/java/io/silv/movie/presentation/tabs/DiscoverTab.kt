package io.silv.movie.presentation.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.silv.movie.MainViewModel
import io.silv.movie.R
import io.silv.movie.presentation.getActivityViewModel
import io.silv.movie.presentation.screen.BrowseListsScreen
import kotlinx.coroutines.flow.receiveAsFlow

data object DiscoverTab: SharedTransitionTab() {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 3u,
            title = stringResource(id = R.string.discover_tab_title),
            icon = rememberVectorPainter(image = Icons.Rounded.VideoLibrary)
        )

    @Composable
    override fun Content() {

        val mainViewModel by getActivityViewModel<MainViewModel>()

        Navigator(BrowseListsScreen) { navigator ->

            LaunchedEffect(Unit) {
                mainViewModel.navigationChannel.receiveAsFlow().collect { action ->
                    with(navigator) { action() }
                }
            }

            AnimatedContentTransition(navigator)
        }
    }
}
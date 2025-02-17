package io.silv.movie.presentation.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.silv.movie.MainViewModel
import io.silv.movie.R
import io.silv.movie.presentation.getActivityViewModel
import io.silv.movie.presentation.screen.ProfileScreen
import kotlinx.coroutines.flow.receiveAsFlow


data object ProfileTab: SharedTransitionTab() {

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 2u,
            title = stringResource(id = R.string.profile_tab_title),
            icon = rememberVectorPainter(image = Icons.Filled.Person)
        )

    @Composable
    override fun Content() {

        val mainViewModel by getActivityViewModel<MainViewModel>()

        Navigator(ProfileScreen) { navigator ->
            LaunchedEffect(Unit) {
                mainViewModel.navigationChannel.receiveAsFlow().collect { action ->
                    with(navigator) { action() }
                }
            }

            AnimatedContentTransition(navigator)
        }
    }
}






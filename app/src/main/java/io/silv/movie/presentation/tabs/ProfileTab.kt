package io.silv.movie.presentation.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.movie.Nav
import io.silv.movie.R
import io.silv.movie.presentation.profile.screen.ProfileScreen


data object ProfileTab: Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 2u,
            title = stringResource(id = R.string.profile_tab_title),
            icon = rememberVectorPainter(image = Icons.Filled.Person)
        )

    @Composable
    override fun Content() {
        Navigator(ProfileScreen) { navigator ->

            SideEffect { Nav.setNav(navigator) }

            FadeTransition(navigator)
        }
    }
}






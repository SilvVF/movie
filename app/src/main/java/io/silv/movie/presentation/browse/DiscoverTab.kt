package io.silv.movie.presentation.browse

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VideoLibrary
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
import io.silv.movie.presentation.browse.lists.BrowseListsScreen

object DiscoverTab: Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 3u,
            title = stringResource(id = R.string.discover_tab_title),
            icon = rememberVectorPainter(image = Icons.Rounded.VideoLibrary)
        )

    @Composable
    override fun Content() {
        val currentNav = LocalGlobalNavigator.current

        Navigator(BrowseListsScreen) {


            SideEffect {
                currentNav.value = it
            }

            FadeTransition(it)
        }
    }
}
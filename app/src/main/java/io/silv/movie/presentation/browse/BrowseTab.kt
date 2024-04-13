package io.silv.movie.presentation.browse

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
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
import io.silv.movie.data.ContentPagedType
import io.silv.movie.presentation.browse.movie.BrowseMovieScreen

data object BrowseTab: Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 0u,
            title = stringResource(id = R.string.browse_tab_title),
            icon = rememberVectorPainter(image = Icons.Rounded.Explore)
        )

    @Composable
    override fun Content() {

        val currentNav = LocalGlobalNavigator.current

        Navigator(BrowseMovieScreen(ContentPagedType.Default.Popular)) {

            SideEffect {
                currentNav.value = it
            }

            FadeTransition(it)
        }
    }
}
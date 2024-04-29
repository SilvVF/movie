package io.silv.movie.presentation.browse

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.silv.movie.Nav
import io.silv.movie.R
import io.silv.movie.data.ContentPagedType
import io.silv.movie.presentation.AnimatedContentTransition
import io.silv.movie.presentation.SharedTransitionTab
import io.silv.movie.presentation.browse.movie.BrowseMovieScreen

data object BrowseTab: SharedTransitionTab() {

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 0u,
            title = stringResource(id = R.string.browse_tab_title),
            icon = rememberVectorPainter(image = Icons.Rounded.Explore)
        )

    @Composable
    override fun Content() {
        Navigator(BrowseMovieScreen(ContentPagedType.Default.Popular)) { navigator ->

            SideEffect { Nav.setNav(navigator) }

            AnimatedContentTransition(navigator, transform = { fadeIn() togetherWith fadeOut() })
        }
    }
}

package io.silv.movie.presentation.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.movie.R
import io.silv.movie.data.ContentPagedType
import io.silv.movie.presentation.browse.movie.BrowseMovieScreen

object BrowseTab: Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 0u,
            title = stringResource(id = R.string.browse_tab_title),
            icon = rememberVectorPainter(image = Icons.Rounded.Explore)
        )

    @Composable
    override fun Content() {

        Navigator(BrowseMovieScreen(ContentPagedType.Default.Popular)) {
            FadeTransition(it)
        }
    }
}
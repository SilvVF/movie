package io.silv.movie

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.silv.movie.presentation.movie.browse.MovieScreen
import io.silv.movie.presentation.movie.discover.MovieDiscoverTab
import kotlinx.collections.immutable.persistentListOf

object TabHost: Screen {

    private val tabs = persistentListOf(
        HomeTab,
        MovieDiscoverTab
    )

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.current

        TabNavigator(MovieDiscoverTab) {
            CompositionLocalProvider(LocalNavigator provides navigator) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0),
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.height(78.dp)
                        ) {
                            IconButton(onClick = { /*TODO*/ }) {
                                Icon(
                                    imageVector = Icons.Filled.Home,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(
                        Modifier
                            .padding(paddingValues)
                            .consumeWindowInsets(paddingValues)
                    ) {
                        CurrentTab()
                    }
                }
            }
        }
    }
}

object HomeTab: Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 0u,
            title = "Home",
            icon = rememberVectorPainter(image = Icons.Filled.Home)
        )

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { navigator.push(MovieScreen()) }
        ) {
            Text("Home", Modifier.align(Alignment.Center))
        }
    }

}
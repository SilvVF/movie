package io.silv.movie

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.silv.core_network.model.movie.MovieVideoResponse
import io.silv.data.movie.interactor.MovieVideo
import io.silv.data.movie.interactor.toDomain
import io.silv.movie.presentation.movie.browse.MovieScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json


enum class DragAnchors {
    Start,
    End,
}

private val NavBarHeight = 72.dp



object TabHost: Screen {

    private val tabs = persistentListOf(
        HomeTab
    )


    private val playerQueue by mutableStateOf<ImmutableList<MovieVideo>?>(
        Json.decodeFromString<MovieVideoResponse>(jsonVideos).results.map { it.toDomain() }.toImmutableList()
    )

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.current
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current

        val state = remember {
            AnchoredDraggableState(
                initialValue = DragAnchors.Start,
                positionalThreshold = { distance: Float -> distance * 0.5f },
                velocityThreshold = { with(density) { 100.dp.toPx() } },
                animationSpec = tween(),
            ).apply {
                updateAnchors(
                    DraggableAnchors {
                        DragAnchors.Start at 0f
                        DragAnchors.End at with(density) { configuration.screenHeightDp.dp.toPx() }
                    }
                )
            }
        }


        val progress by animateFloatAsState(
            targetValue = (1 - (state.requireOffset() / state.anchors.maxAnchor())).coerceIn(0f..1f),
            label = ""
        )

        TabNavigator(HomeTab) {

            CompositionLocalProvider(LocalNavigator provides navigator) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0),
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.height(NavBarHeight * (1f - progress))
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
                    Box(Modifier.fillMaxSize()) {
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
}

@Stable
private data class ScrollHolder(
    var idx: Int,
    var offset: Int
)

@Composable
fun BoxScope.MediaMotionLayout() {


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
            modifier = Modifier.fillMaxSize()
        ) {
            MovieScreen().Content()
        }
    }
}

private val jsonVideos = """
    {
      "id": 121,
      "results": [
        {
          "iso_639_1": "en",
          "iso_3166_1": "US",
          "name": "Lord of the Rings - The Two Towers (2002) Theatrical Trailer [5.1] [4K] [FTD-1357]",
          "key": "HUkDW37WaI0",
          "site": "YouTube",
          "size": 2160,
          "type": "Trailer",
          "official": false,
          "published_at": "2023-12-18T06:00:11.000Z",
          "id": "657fef14226c5608599dd253"
        },
        {
          "iso_639_1": "en",
          "iso_3166_1": "US",
          "name": "The Two Towers | The Lord of the Rings 4K Ultra HD | Warner Bros. Entertainment",
          "key": "nuTU5XcZTLA",
          "site": "YouTube",
          "size": 2160,
          "type": "Trailer",
          "official": true,
          "published_at": "2020-12-02T17:34:35.000Z",
          "id": "5fc8aafe3f8ede004000808c"
        },
        {
          "iso_639_1": "en",
          "iso_3166_1": "US",
          "name": "The Lord Of The Rings: The Two Towers - 10 Minute Preview - Warner Bros. UK",
          "key": "lgEfUC8md88",
          "site": "YouTube",
          "size": 1080,
          "type": "Clip",
          "official": true,
          "published_at": "2020-06-05T14:14:55.000Z",
          "id": "615f954f43d9b10063a98f07"
        },
        {
          "iso_639_1": "en",
          "iso_3166_1": "US",
          "name": "The Lord of the Rings: The Two Towers (2002) Teaser",
          "key": "oi16WZBkaGs",
          "published_at": "2018-09-10T02:24:01.000Z",
          "site": "YouTube",
          "size": 1080,
          "type": "Teaser",
          "official": false,
          "id": "5b95d6e3c3a36856810329c3"
        },
        {
          "iso_639_1": "en",
          "iso_3166_1": "US",
          "name": "Lord of the Rings: The Two Towers - Trailer",
          "key": "cvCktPUwkW0",
          "published_at": "2012-04-11T21:25:08.000Z",
          "site": "YouTube",
          "size": 1080,
          "type": "Teaser",
          "official": false,
          "id": "5b43c4c59251414f3901ded0"
        }
        ,
        {
          "iso_639_1": "en",
          "iso_3166_1": "US",
          "name": "Lord of the Rings: The Two Towers - Trailer",
          "key": "cvCktPUwkW0",
          "published_at": "2012-04-11T21:25:08.000Z",
          "site": "YouTube",
          "size": 1080,
          "type": "Teaser",
          "official": false,
          "id": "5b43c51414f3901ded0"
        }
        ,
        {
          "iso_639_1": "en",
          "iso_3166_1": "US",
          "name": "Lord of the Rings: The Two Towers - Trailer",
          "key": "cvCktPUwkW0",
          "published_at": "2012-04-11T21:25:08.000Z",
          "site": "YouTube",
          "size": 1080,
          "type": "Teaser",
          "official": false,
          "id": "5b43c4414f3ed0"
        },
        {
          "iso_639_1": "en",
          "iso_3166_1": "US",
          "name": "Lord of the Rings: The Two Towers - Trailer",
          "key": "cvCktPUwkW0",
          "published_at": "2012-04-11T21:25:08.000Z",
          "site": "YouTube",
          "size": 1080,
          "type": "Teaser",
          "official": false,
          "id": "5b43"
        }
      ]
    }
""".trimIndent()
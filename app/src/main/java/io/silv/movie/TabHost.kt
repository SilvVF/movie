package io.silv.movie

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.core_network.model.movie.MovieVideoResponse
import io.silv.data.movie.interactor.MovieVideo
import io.silv.data.movie.interactor.toDomain
import io.silv.movie.presentation.media.YoutubeVideoPlayer
import io.silv.movie.presentation.movie.browse.MovieScreen
import io.silv.movie.presentation.movie.view.components.VideoMediaItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json


private val NavBarHeight = 72.dp

class MainScreenModel: ScreenModel {

    var videos by mutableStateOf<ImmutableList<MovieVideo>?>(null)
        private set

    var collapsableVideoState: CollapsableVideoState? = null

    val list = Json.decodeFromString<MovieVideoResponse>(jsonVideos).results
        .map { it.toDomain() }
        .toImmutableList()

    fun requestMediaQueue() {
        videos = list
    }

    fun clearMediaQueue() {
        Log.d("Clear", "Clearing")
        videos = null
        screenModelScope.launch {
            collapsableVideoState?.state?.snapTo(CollapsableVideoAnchors.Start)
        }
    }
}

object TabHost: Screen {

    private val tabs = persistentListOf(HomeTab)


    @Composable
    override fun Content() {

        val navigator = LocalNavigator.current

        val mainScreenModel = getScreenModel<MainScreenModel>()
        val collapsableVideoState = rememberCollapsableVideoState()

        DisposableEffect(collapsableVideoState) {
            mainScreenModel.collapsableVideoState = collapsableVideoState
            onDispose {
                mainScreenModel.collapsableVideoState = null
            }
        }

        BackHandler(
            enabled = mainScreenModel.videos.isNullOrEmpty().not()
        ) {
            mainScreenModel.clearMediaQueue()
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(top = 0.dp, bottom = 0.dp, left = 0.dp, right = 0.dp),
            bottomBar = {
                NavigationBar(
                    Modifier.height(
                        if (mainScreenModel.videos.isNullOrEmpty()) {
                            NavBarHeight
                        } else {
                            NavBarHeight * (1f - collapsableVideoState.progress)
                        }
                    )
                ) {}
            }
        ) { paddingValues ->
            Box(
                Modifier
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
            ) {
                Navigator(MovieScreen()) {
                    FadeTransition(it)
                }

                AnimatedVisibility(
                    visible = mainScreenModel.videos.isNullOrEmpty().not(),
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.BottomCenter),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = fadeOut()
                ) {
                    CollapsableVideo(
                        state = collapsableVideoState,
                        videos = mainScreenModel.videos ?: return@AnimatedVisibility,
                        onDismissRequested = mainScreenModel::clearMediaQueue
                    )
                }
            }
        }
    }
}

@Composable
fun BoxScope.CollapsableVideo(
    state: CollapsableVideoState,
    videos: ImmutableList<MovieVideo>,
    onDismissRequested: () -> Unit
) {
    CollapsableVideoLayout(
        actions = {
            Text(
                text = "Title text",
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { /*TODO*/ }
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null
                )
            }

            IconButton(
                onClick = { /*TODO*/ }
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null
                )
            }
        },
        collapsableVideoState = state,
        modifier = Modifier,
        player = {
            YoutubeVideoPlayer(
                Modifier,
                videoId = "HUkDW37WaI0"
            )
        },
        onDismissRequested = onDismissRequested
    ) {
        items(videos) {
            VideoMediaItem(onThumbnailClick = { /*TODO*/ }, item = it) {
                if (it.site == "YouTube") {
                    "https://img.youtube.com/vi/${it.key}/0.jpg"
                } else {
                    ""
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
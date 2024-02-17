package io.silv.movie

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.CurveFit
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.silv.core_network.model.movie.MovieVideoResponse
import io.silv.core_ui.components.FastScrollLazyColumn
import io.silv.data.movie.interactor.MovieVideo
import io.silv.data.movie.interactor.toDomain
import io.silv.movie.presentation.media.YoutubeVideoPlayer
import io.silv.movie.presentation.movie.browse.MovieScreen
import io.silv.movie.presentation.movie.view.components.VideoMediaItem
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
                        playerQueue?.let { videos ->
                            MediaMotionLayout(
                                videos,
                                paddingValues,
                                progress,
                                state
                            )
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun BoxScope.MediaMotionLayout(
    videos: ImmutableList<MovieVideo>,
    paddingValues: PaddingValues,
    progress: Float,
    state: AnchoredDraggableState<DragAnchors>
) {
    val motionScene = remember {
        MotionScene {

            val container = createRefFor("video_overlay_touchable_area")
            val thumbnail = createRefFor("video_overlay_thumbnail")
            val title = createRefFor("video_overlay_title")
            val items = createRefFor("items")
            val itemsBg = createRefFor("items_solid_background")

            defaultTransition(
                from = constraintSet {
                    constrain(container) {
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                        bottom.linkTo(thumbnail.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(thumbnail.top)
                    }
                    constrain(itemsBg) {
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                        start.linkTo(items.start)
                        end.linkTo(items.end)
                        top.linkTo(items.top)
                        bottom.linkTo(items.bottom)
                    }
                    constrain(items) {
                        width = Dimension.matchParent
                        height = Dimension.value(0.dp)
                        alpha = 0.4f
                        top.linkTo(thumbnail.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    }
                    constrain(thumbnail) {
                        width = Dimension.percent(0.25f)
                        height = Dimension.percent(0.08f)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                    }
                    constrain(title) {
                        width = Dimension.fillToConstraints
                        height = Dimension.wrapContent
                        start.linkTo(thumbnail.end, margin = 4.dp)
                        bottom.linkTo(thumbnail.bottom)
                        end.linkTo(parent.end,margin = 4.dp)
                        top.linkTo(thumbnail.top)
                    }
                },
                to = constraintSet {
                    constrain(container) {
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                        bottom.linkTo(thumbnail.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(thumbnail.top)
                    }
                    constrain(thumbnail) {
                        width = Dimension.matchParent
                        height = Dimension.ratio("16:9")
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    }
                    constrain(title) {
                        width = Dimension.fillToConstraints
                        height = Dimension.wrapContent
                        alpha = 0f
                        start.linkTo(thumbnail.end, margin = 4.dp)
                        bottom.linkTo(thumbnail.bottom)
                        end.linkTo(parent.end, margin = 4.dp)
                        top.linkTo(thumbnail.top)
                    }
                    constrain(items) {
                        width = Dimension.matchParent
                        height = Dimension.fillToConstraints
                        top.linkTo(thumbnail.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    }
                    constrain(itemsBg) {
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                        start.linkTo(items.start)
                        end.linkTo(items.end)
                        top.linkTo(items.top)
                        bottom.linkTo(items.bottom)
                    }
                }
            ) {
                keyPositions(thumbnail) {
                    frame(50) {
                        percentWidth = 1f
                        percentX = 0.5f
                        curveFit = CurveFit.Linear
                    }
                }
                keyPositions(title) {
                    frame(30) {
                        percentWidth = 1f
                        percentX = 1f
                    }
                }
                keyAttributes(title) {
                    frame(30) {
                        alpha = 0f
                    }
                }
            }
        }
    }
    val surfaceColor =  MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)

    MotionLayout(
        motionScene = motionScene,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .drawWithContent {
                drawContent()
                drawLine(
                    color = Color.Gray,
                    strokeWidth = 2f,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height)
                )
            }
            .align(Alignment.BottomCenter)
            .windowInsetsPadding(WindowInsets.statusBars),
        progress = progress
    ) {
        Box(
            modifier = Modifier
                .drawBehind { drawRect(surfaceColor) }
                .layoutId("items_solid_background"),
        )
        FastScrollLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .layoutId("items"),
        ) {
            items(videos) {
                VideoMediaItem(
                    onThumbnailClick = { /*TODO*/ },
                    item = it,
                    thumbnailProvider = {
                        if (it.site == "YouTube") {
                            "https://img.youtube.com/vi/${it.key}/0.jpg"
                        } else {
                            ""
                        }
                    }
                )
            }
        }
        Box(
            modifier = Modifier
                .clipToBounds()
                .drawBehind { drawRect(surfaceColor) }
                .anchoredDraggable(
                    state,
                    Orientation.Vertical
                )
                .layoutId("video_overlay_touchable_area"),
        )
        Row(
            Modifier
                .layoutId("video_overlay_title"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = videos.first().name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = { /*TODO*/ }) {
                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
            }
            IconButton(onClick = { /*TODO*/ }) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = null)
            }
        }
        Box(
            Modifier
                .layoutId("video_overlay_thumbnail")
                .fillMaxWidth()
                .clipToBounds()
                .anchoredDraggable(
                    state,
                    Orientation.Vertical
                )
        ) {
            YoutubeVideoPlayer(
                modifier = Modifier.matchParentSize(),
                videoId = "nuTU5XcZTLA"
            )
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
      ]
    }
""".trimIndent()
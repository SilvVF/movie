package io.silv.movie.presentation.view.movie

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import io.silv.core_ui.components.PullRefresh
import io.silv.core_ui.components.VerticalFastScroller
import io.silv.core_ui.util.copyToClipboard
import io.silv.movie.PlayerViewModel
import io.silv.movie.getActivityViewModel
import io.silv.movie.presentation.toPoster
import io.silv.movie.presentation.view.components.MovieInfoBox
import io.silv.movie.presentation.view.movie.components.ExpandableMovieDescription
import io.silv.movie.presentation.view.movie.components.VideoMediaItem
import org.koin.core.parameter.parametersOf

data class MovieViewScreen(
    val id: Long,
): Screen {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<MovieViewScreenModel> { parametersOf(id) }
        val mainScreenModel = getActivityViewModel<PlayerViewModel>()

        when (val state = screenModel.state.collectAsStateWithLifecycle().value) {
            MovieDetailsState.Error ->  Box(modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            MovieDetailsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            is MovieDetailsState.Success -> {
                MovieDetailsContent(
                    state = state,
                    refresh = screenModel::refresh,
                    onVideoThumbnailClick = mainScreenModel::requestMediaQueue
                )
            }
        }
    }
}

@Composable
fun MovieDetailsContent(
    state: MovieDetailsState.Success,
    refresh: () -> Unit,
    onVideoThumbnailClick: (movieId: Long, isMovie: Boolean,  trailerId: Long) -> Unit
) {
    Scaffold { paddingValues ->

        val topPadding = paddingValues.calculateTopPadding()
        val listState = rememberLazyListState()
        val hazeState = remember { HazeState() }

        PullRefresh(
            refreshing = state.refreshing,
            onRefresh = refresh,
            enabled = { true },
            indicatorPadding = PaddingValues(top = topPadding),
        ) {
            val layoutDirection = LocalLayoutDirection.current
            VerticalFastScroller(
                listState = listState,
                topContentPadding = topPadding,
                endContentPadding = paddingValues.calculateEndPadding(layoutDirection),
                bottomContentPadding = paddingValues.calculateBottomPadding(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(
                            state = hazeState,
                            style = HazeDefaults.style(MaterialTheme.colorScheme.background),
                        ),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        end = paddingValues.calculateEndPadding(layoutDirection),
                        bottom = paddingValues.calculateBottomPadding()
                    ),
                ) {
                    item(
                        key = "Info-Box",
                        contentType = "Info-Box",
                    ) {
                        MovieInfoBox(
                            isTabletUi = false,
                            appBarPadding = topPadding,
                            title = state.movie.title,
                            author = remember(state.movie.productionCompanies) {
                                state.movie.productionCompanies?.joinToString()
                            },
                            artist = "",
                            sourceName = "TMDB",
                            isStubSource = false,
                            coverDataProvider = { state.movie.toPoster() },
                            status = state.movie.status,
                            onCoverClick = { },
                            doSearch = { _, _ -> },
                        )
                    }
                    item("Description-Tags") {
                        val context = LocalContext.current
                        ExpandableMovieDescription(
                            defaultExpandState = false,
                            description = state.movie.overview,
                            tagsProvider = { state.movie.genres },
                            onTagSearch = {},
                            onCopyTagToClipboard = {
                                context.copyToClipboard("tag", it)
                            }
                        )
                    }
                    items(
                        items = state.trailers,
                        key = { it.id }
                    ) {
                        VideoMediaItem(
                            onThumbnailClick = {
                                onVideoThumbnailClick(it.contentId, true, it.id)
                            },
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
            }
        }
    }
}


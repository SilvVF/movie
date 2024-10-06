package io.silv.movie.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExploreOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.EmptyScreen
import io.silv.core_ui.components.ItemCover
import io.silv.core_ui.components.lazy.VerticalFastScroller
import io.silv.core_ui.components.topbar.PosterLargeTopBar
import io.silv.core_ui.components.topbar.colors2
import io.silv.core_ui.components.topbar.rememberPosterTopBarState
import io.silv.core_ui.util.rememberDominantColor
import io.silv.core_ui.voyager.ScreenResult
import io.silv.core_ui.voyager.ScreenWithResult
import io.silv.core_ui.voyager.setScreenResult
import io.silv.movie.R
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.koin4ScreenModel
import io.silv.movie.presentation.components.content.ContentListPoster
import io.silv.movie.presentation.components.content.ContentListPreview
import io.silv.movie.presentation.screenmodel.AddToListScreenModel
import kotlinx.parcelize.Parcelize
import org.koin.core.parameter.parametersOf

data class AddToListScreen(
    val contentId: Long,
    val isMovie: Boolean
): ScreenWithResult<AddToListScreen.ListResult> {

    override val key: ScreenKey
        get() = "$contentId$isMovie"

    @Parcelize
    data class ListResult(
        val listId: Long,
    ): ScreenResult

    @Composable
    override fun Content() {
        val screenModel = koin4ScreenModel<AddToListScreenModel> {
            parametersOf(
                contentId,
                isMovie
            )
        }

        val poster by screenModel.state.collectAsStateWithLifecycle()
        val lists by screenModel.lists.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.currentOrThrow
        val hazeState = remember { HazeState() }

        val primary by rememberDominantColor(data = poster)
        val background = MaterialTheme.colorScheme.background
        val state = rememberPosterTopBarState()

        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize()
                        .drawWithCache {
                            onDrawBehind {
                                drawRect(
                                    Brush.verticalGradient(
                                        colors = listOf(primary, background),
                                        endY = size.height * 0.8f
                                    ),
                                    alpha = if (state.isKeyboardOpen) 0f else 1f - state.progress
                                )
                            }
                        }
                        .hazeChild(hazeState),
                ) {
                    PosterLargeTopBar(
                        state = state,
                        maxHeight = 284.dp,
                        title = {
                            Text(
                                text = stringResource(
                                    R.string.add_to_a_list, poster?.title.orEmpty()
                                ),
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .padding(end = 22.dp)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null
                                )
                            }
                        },
                        colors = TopAppBarDefaults.colors2(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = primary.copy(alpha = 0.2f)
                        ),
                        posterContent = {
                            ItemCover.Square(
                                data = poster,
                                modifier = Modifier
                                    .fillMaxHeight()
                            )
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(state.scrollBehavior.nestedScrollConnection)
        ) { paddingValues ->
            if (lists.isEmpty()) {
                EmptyScreen(
                    icon = Icons.Filled.ExploreOff,
                    iconSize = 182.dp,
                    contentPadding = paddingValues,
                    message = "No lists in you library that don't already contain this item"
                )
            } else {
                LibraryLists(
                    paddingValues,
                    lists,
                    onListClick = {
                        setScreenResult(ListResult(it.id))
                        navigator.pop()
                    },
                    onListLongClick = {
                        setScreenResult(ListResult(it.id))
                        navigator.pop()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(hazeState, HazeDefaults.style(MaterialTheme.colorScheme.background))
                )
            }
        }
    }
}

@Composable
private fun LibraryLists(
    paddingValues: PaddingValues,
    lists: List<Pair<ContentList, List<ContentItem>>>,
    onListLongClick: (contentList: ContentList) -> Unit,
    onListClick: (contentList: ContentList) -> Unit,
    modifier: Modifier = Modifier,
) {
    val topPadding = paddingValues.calculateTopPadding()
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current

    VerticalFastScroller(
        listState = listState,
        topContentPadding = topPadding,
        endContentPadding = paddingValues.calculateEndPadding(layoutDirection),
        bottomContentPadding = paddingValues.calculateBottomPadding(),
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize(),
            state = listState,
            contentPadding = paddingValues,
        ) {
            lists.fastForEach { (list, items) ->
                item(
                    list.id
                ) {
                    ContentListPreview(
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = { onListLongClick(list) },
                                onClick = { onListClick(list) }
                            )
                            .animateItemPlacement()
                            .padding(8.dp),
                        cover = {
                            ContentListPoster(
                                list = list,
                                items = items,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onListClick(list) }
                            )
                        },
                        name = list.name,
                        description = list.description.ifEmpty {
                            when {
                                items.isEmpty() -> stringResource(id = R.string.content_preview_no_items)
                                else -> stringResource(R.string.content_preview_items, items.size)
                            }
                        }
                    )
                }
            }
        }
    }
}

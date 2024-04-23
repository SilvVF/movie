package io.silv.movie.presentation.library.screens

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
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.EmptyScreen
import io.silv.core_ui.components.ItemCover
import io.silv.core_ui.components.PosterData
import io.silv.core_ui.components.lazy.VerticalFastScroller
import io.silv.core_ui.components.topbar.PosterLargeTopBar
import io.silv.core_ui.components.topbar.colors2
import io.silv.core_ui.components.topbar.rememberPosterTopBarState
import io.silv.core_ui.util.rememberDominantColor
import io.silv.core_ui.voyager.ScreenResult
import io.silv.core_ui.voyager.ScreenWithResult
import io.silv.core_ui.voyager.setScreenResult
import io.silv.movie.R
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListItem
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.presentation.library.components.ContentListPoster
import io.silv.movie.presentation.library.components.ContentListPreview
import io.silv.movie.presentation.toPoster
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import org.koin.core.parameter.parametersOf

class AddToListScreenModel(
    contentListRepository: ContentListRepository,
    getMovie: GetMovie,
    getShow: GetShow,
    contentId: Long,
    isMovie: Boolean,
): StateScreenModel<PosterData?>(null) {

    init {
        val posterDataFlow = if (isMovie) {
            getMovie.subscribePartial(contentId)
                .onEach { movie ->
                    mutableState.update { movie.toPoster() }
                }
        } else {
            getShow.subscribePartial(contentId)
                .onEach { show ->
                    mutableState.update { show.toPoster() }
                }
        }

        posterDataFlow.launchIn(screenModelScope)
    }

    val lists = contentListRepository.observeLibraryItems("")
        .map { contentListItems ->
            contentListItems.groupBy { it.list }
                .mapValues { (_, items) ->
                    items.filterIsInstance<ContentListItem.Item>()
                        .map { item -> item.contentItem }
                        .toImmutableList()
                }
                .toList()
                .filterNot { (_, items) ->
                    items.any { item ->
                        item.isMovie == isMovie && item.contentId == contentId
                    }
                }
                .toImmutableList()
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            persistentListOf()
        )
}

@Composable
private fun LibraryLists(
    paddingValues: PaddingValues,
    lists: ImmutableList<Pair<ContentList, ImmutableList<ContentItem>>>,
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
                    key = list.id
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


data class AddToListScreen(
    val contentId: Long,
    val isMovie: Boolean
): ScreenWithResult<AddToListScreen.ListResult> {

    @Parcelize
    data class ListResult(
        val listId: Long,
    ): ScreenResult

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<AddToListScreenModel> { parametersOf(contentId, isMovie) }

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
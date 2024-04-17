package io.silv.movie.presentation.browse.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExploreOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.map
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc
import io.silv.core_ui.components.Action
import io.silv.core_ui.components.EmptyScreen
import io.silv.core_ui.components.NoResultsEmptyScreen
import io.silv.core_ui.components.loadingIndicatorItem
import io.silv.core_ui.components.topbar.SearchLargeTopBar
import io.silv.core_ui.components.topbar.colors2
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.R
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.presentation.library.components.ContentListPosterItems
import io.silv.movie.presentation.library.components.topbar.PosterLargeTopBarDefaults
import io.silv.movie.presentation.library.screens.ListViewScreen
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable

class ListSearchPagingSource(
    private val postgrest: Postgrest,
    private val query: String,
): PagingSource<Int, ListWithPostersRpcResponse>() {

    @Serializable
    data class Params(
        val query: String,
        val off: Int,
        val lim: Int
    )

    override fun getRefreshKey(state: PagingState<Int, ListWithPostersRpcResponse>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListWithPostersRpcResponse> {

        return try {
            val offset = (params.key ?: 0) * params.loadSize
            val limit = params.loadSize


            val result = postgrest.rpc(
                "select_lists_with_poster_items_for_query",
                Params("%$query%", offset, limit)
            )
                .decodeList<ListWithPostersRpcResponse>()

            val nextStart = offset + limit

            LoadResult.Page(
                data = result,
                prevKey = params.key?.minus(1),
                nextKey = (params.key ?: 0).plus(1).takeIf {
                    nextStart <= (result.first().total ?: Long.MAX_VALUE) && result.size >= params.loadSize
                }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

}

class SearchForListScreenModel(
    private val postgrest: Postgrest,
    private val contentListRepository: ContentListRepository,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
): ScreenModel {

    var query by mutableStateOf("")

    val state = snapshotFlow { query }
        .debounce(500L)
        .filter { it.isNotBlank() }
        .flatMapLatest {
            Pager(
                config = PagingConfig(pageSize = 30)
            ) {
                ListSearchPagingSource(postgrest, it)
            }
                .flow.cachedIn(ioCoroutineScope).map { pagingData ->
                    pagingData.map {
                        it.toListPreviewItem(contentListRepository, getShow, getMovie)
                    }
                }
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PagingData.empty()
        )
}

data object SearchForListScreen: Screen {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<SearchForListScreenModel>()
        val pagingItems = screenModel.state.collectAsLazyPagingItems()
        val navigator = LocalNavigator.currentOrThrow
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

        val hazeState = remember { HazeState() }

        Scaffold(
            topBar = {
                SearchLargeTopBar(
                    title = { Text(text = "Search lists") },
                    colors = TopAppBarDefaults.colors2(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    maxHeight = 164.dp,
                    extraContent = {},
                    pinnedContent = {
                        PosterLargeTopBarDefaults.SearchInputField(
                            query = { screenModel.query },
                            onSearch = { screenModel.query = it },
                            changeQuery = { screenModel.query = it },
                            placeholder = stringResource(id = R.string.search_placeholder, "for lists")
                        )
                    },
                    scrollBehavior = scrollBehavior,
                    modifier = Modifier.hazeChild(hazeState)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { paddingValues ->

            if (screenModel.query.isBlank()) {
                EmptyScreen(
                    icon = Icons.Filled.SearchOff,
                    iconSize = 176.dp,
                    message = stringResource(id = R.string.enter_a_query),
                    contentPadding = paddingValues,
                )
                return@Scaffold
            }

            if (pagingItems.loadState.refresh is LoadState.Loading) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                return@Scaffold
            }

            if (pagingItems.loadState.refresh is LoadState.Error) {
                EmptyScreen(
                    icon = Icons.Filled.ExploreOff,
                    iconSize = 176.dp,
                    message = stringResource(id = io.silv.core_ui.R.string.no_results_found),
                    contentPadding = paddingValues,
                    actions = remember {
                        persistentListOf(Action(R.string.retry){ pagingItems.retry() })
                    }
                )
                return@Scaffold
            }

            if (pagingItems.itemCount == 0) {
                NoResultsEmptyScreen(contentPaddingValues = paddingValues)
                return@Scaffold
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(128.dp),
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxSize()
                    .haze(hazeState, HazeDefaults.style(MaterialTheme.colorScheme.background)),
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.list.id },
                ) {
                    val item = pagingItems[it] ?: return@items
                    RowPreviewItem(
                        modifier = Modifier
                            .clickable {
                                navigator.push(
                                    ListViewScreen(
                                        item.list.id,
                                        item.list.supabaseId.orEmpty()
                                    )
                                )
                            }
                            .padding(12.dp),
                        cover = {
                            ContentListPosterItems(
                                list = item.list,
                                items = item.items,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            )
                        },
                        name = item.list.name,
                    )
                }
                loadingIndicatorItem(pagingItems)
            }
        }
    }
}
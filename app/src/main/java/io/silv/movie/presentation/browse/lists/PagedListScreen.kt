package io.silv.movie.presentation.browse.lists

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExploreOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc
import io.silv.core_ui.components.Action
import io.silv.core_ui.components.EmptyScreen
import io.silv.core_ui.components.NoResultsEmptyScreen
import io.silv.core_ui.components.loadingIndicatorItem
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
import io.silv.movie.LocalUser
import io.silv.movie.R
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.user.FromSubscribedRpcParams
import io.silv.movie.presentation.LocalListInteractor
import io.silv.movie.presentation.library.components.ContentListPosterStateFlowItems
import io.silv.movie.presentation.library.components.dialog.ListOptionsBottomSheet
import io.silv.movie.presentation.library.screens.ListAddScreen
import io.silv.movie.presentation.library.screens.ListEditDescriptionScreen
import io.silv.movie.presentation.library.screens.ListEditScreen
import io.silv.movie.presentation.library.screens.ListViewScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.parameter.parametersOf
import timber.log.Timber


enum class ListPagedType(val v: Int) {
    Recent(1),
    MoreFromSubscribed(2),
    Popular(3)
}

class ListPagedScreenModel(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val pagedType: ListPagedType,
    private val contentListRepository: ContentListRepository,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
): ScreenModel {

    private inner class ListPagingSource: PagingSource<Int, ListWithPostersRpcResponse>() {

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

                val rpc = when (pagedType) {
                    ListPagedType.MoreFromSubscribed -> postgrest.rpc(
                        "select_recommended_by_subscriptions",
                        FromSubscribedRpcParams(uid = auth.currentUserOrNull()?.id!!, off = offset,lim = limit)
                    )
                    ListPagedType.Popular -> postgrest.rpc(
                        "select_most_popular_lists_with_poster_items",
                        PopularListParams(lim = limit, off = offset)
                    )
                    ListPagedType.Recent -> postgrest.rpc(
                        "select_most_recent_lists_with_poster_items",
                        PopularListParams(lim = limit, off = offset)
                    )
                }

                val result = rpc.decodeList<ListWithPostersRpcResponse>()

                val nextStart = offset + limit

                LoadResult.Page(
                    data = result,
                    prevKey = params.key?.minus(1),
                    nextKey = (params.key ?: 0).plus(1).takeIf {
                        nextStart <= (result.first().total ?: Long.MAX_VALUE) && result.size >= params.loadSize
                    }
                )
            } catch (e: Exception) {
                Timber.d(e)
                LoadResult.Error(e)
            }
        }
    }

    val pagingData =  Pager(
        config = PagingConfig(pageSize = 30)
    ) {
        ListPagingSource()
    }
        .flow.map { pagingData ->
            pagingData.uniqueBy { it.listId }.map {
                it.toListPreviewItem(contentListRepository, getShow, getMovie, ioCoroutineScope)
            }
        }
        .cachedIn(ioCoroutineScope)
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PagingData.empty()
        )
}

data class PagedListScreen(
    val pagedType: ListPagedType
): Screen {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<ListPagedScreenModel> { parametersOf(pagedType) }
        val pagingItems = screenModel.pagingData.collectAsLazyPagingItems()
        val listInteractor = LocalListInteractor.current
        val navigator = LocalNavigator.currentOrThrow
        var selectedList by remember {
            mutableStateOf<Pair<ContentList, ImmutableList<ContentItem>>?>(null)
        }

        val hazeState = remember { HazeState() }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (pagedType){
                                ListPagedType.MoreFromSubscribed -> stringResource(id = R.string.more_from_subscribed)
                                ListPagedType.Popular -> stringResource(id = R.string.popular)
                                ListPagedType.Recent -> stringResource(id = R.string.recently_created)
                            }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    modifier = Modifier.hazeChild(hazeState)
                )
            },
            modifier = Modifier
                .fillMaxSize()
        ) { paddingValues ->

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

            val selectList = { item: ListPreviewItem ->
                selectedList = item.list to item.items.mapNotNull { it.value }.toImmutableList()
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
                    key = pagingItems.itemKey { "${it.list.id}${it.list.supabaseId}" },
                ) {
                    val item = pagingItems[it] ?: return@items
                    RowPreviewItem(
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = { selectList(item) }
                            ) {
                                navigator.push(
                                    ListViewScreen(
                                        item.list.id,
                                        item.list.supabaseId.orEmpty()
                                    )
                                )
                            }
                            .padding(12.dp),
                        cover = {
                            ContentListPosterStateFlowItems(
                                list = item.list,
                                items = item.items,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .height(120.dp)
                            )
                        },
                        name = item.list.name,
                    )
                }
                loadingIndicatorItem(pagingItems)
            }
        }

        selectedList?.let { (list, items) ->
            val listEditScreen = remember(list.name) { ListEditScreen(list.name) }

            val editResultLauncher = rememberScreenWithResultLauncher(
                screen = listEditScreen
            ) { result ->
                listInteractor.editList(list) { it.copy(name = result.name) }
            }

            val descriptionEditScreen =
                remember(list.description) { ListEditDescriptionScreen(list.description) }

            val descriptionResultLauncher = rememberScreenWithResultLauncher(
                screen = descriptionEditScreen
            ) { result ->
                listInteractor.editList(list) { it.copy(description = result.description) }
            }

            ListOptionsBottomSheet(
                onDismissRequest = { selectedList = null},
                onAddClick = { navigator.push(ListAddScreen(list.id)) },
                onEditClick = {
                    editResultLauncher.launch()
                    selectedList = null
                },
                onDeleteClick = { listInteractor.deleteList(list) },
                onShareClick = {
                    listInteractor.toggleListVisibility(list)
                    selectedList = null
                },
                list = list,
                onChangeDescription = { descriptionResultLauncher.launch() },
                onCopyClick = {
                    listInteractor.copyList(list)
                    selectedList = null
                },
                isUserMe = list.createdBy == LocalUser.current?.userId || list.createdBy == null,
                content = items ,
                onSubscribeClicked = {
                    listInteractor.subscribeToList(list)
                    selectedList = null
                },
                onUnsubscribeClicked = {
                    listInteractor.unsubscribeFromList(list)
                    selectedList = null
                }
            )
        }
    }
}
package io.silv.movie.presentation.screen

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExploreOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.paging.map
import app.cash.paging.PagingConfig
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.jan.supabase.postgrest.Postgrest
import io.silv.core_ui.components.Action
import io.silv.core_ui.components.EmptyScreen
import io.silv.core_ui.components.ItemCover
import io.silv.core_ui.components.NoResultsEmptyScreen
import io.silv.core_ui.components.PageLoadingIndicator
import io.silv.core_ui.components.lazy.ScrollbarLazyColumn
import io.silv.core_ui.components.shimmer.ButtonPlaceholder
import io.silv.core_ui.components.shimmer.ShimmerHost
import io.silv.core_ui.components.shimmer.TextPlaceholder
import io.silv.core_ui.components.topbar.SearchLargeTopBar
import io.silv.core_ui.components.topbar.colors2
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
import io.silv.movie.MovieTheme
import io.silv.movie.R
import io.silv.movie.data.content.lists.ListWithPostersRpcResponse
import io.silv.movie.data.content.lists.repository.ContentListRepository
import io.silv.movie.data.content.lists.toListPreviewItem
import io.silv.movie.data.content.movie.interactor.GetMovie
import io.silv.movie.data.content.tv.interactor.GetShow
import io.silv.movie.data.user.SupabaseConstants
import io.silv.movie.data.user.User
import io.silv.movie.data.user.repository.UserRepository
import io.silv.movie.koin4ScreenModel
import io.silv.movie.presentation.LocalListInteractor
import io.silv.movie.presentation.ProvideLocalsForPreviews
import io.silv.movie.presentation.components.content.ContentListPosterStateFlowItems
import io.silv.movie.presentation.components.content.ContentListPreview
import io.silv.movie.presentation.components.dialog.ListOptionsBottomSheet
import io.silv.movie.presentation.components.profile.ProfileTopBar
import io.silv.movie.presentation.screen.ProfileViewState.Loading
import io.silv.movie.presentation.screenmodel.ListPreviewItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

sealed interface ProfileViewState {
    data object Loading: ProfileViewState
    data class Success(val user: User): ProfileViewState
    data class Failure(val message: String): ProfileViewState
}


class ProfileViewScreenModel(
    private val postgrest: Postgrest,
    private val userRepository: UserRepository,
    private val contentListRepository: ContentListRepository,
    private val getShow: GetShow,
    private val getMovie: GetMovie,
    val userId: String
): StateScreenModel<ProfileViewState>(Loading) {

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true)
            return

        refreshJob = screenModelScope.launch {
            val user = loadUser(userId)

            if (user == null){
                mutableState.update { ProfileViewState.Failure("No user found") }
                return@launch
            }

            mutableState.update { ProfileViewState.Success(user) }
        }
    }
    
    private suspend fun loadUser(userId: String): User? {
        return userRepository.getUser(userId)
    }
    
    val userListsPagingData = Pager(
        config = PagingConfig(20),
    ) {
        UserListPagingSource()
    }.flow.map { pagingData ->
        pagingData.map { response ->
            response.toListPreviewItem(contentListRepository, getShow, getMovie, screenModelScope)
        }
    }
        .cachedIn(screenModelScope)
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PagingData.empty()
        )

    private inner class UserListPagingSource(): PagingSource<Int, ListWithPostersRpcResponse>() {

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


                val result = SupabaseConstants.RPC.selectListsWithPosterItems(
                    postgrest,
                    limit,
                    offset,
                    userId
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
}

data class ProfileViewScreen(
    val userId: String
): Screen {

    override val key: ScreenKey = "profile_$userId"

    @Composable
    override fun Content() {
        val screenModel = koin4ScreenModel<ProfileViewScreenModel> { parametersOf(userId) }

        val navigator = LocalNavigator.currentOrThrow
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        when (val state = screenModel.state.collectAsStateWithLifecycle().value) {
            is ProfileViewState.Failure ->   EmptyScreen(
                icon = Icons.Filled.ExploreOff,
                iconSize = 182.dp,
                actions = listOf(
                    Action(
                        R.string.retry,
                        onClick = screenModel::refresh
                    )
                )
            )
            Loading -> ProfileViewLoadingScreen(scrollBehavior)
            is ProfileViewState.Success -> {
                val pagingData = screenModel.userListsPagingData.collectAsLazyPagingItems()

                ProfileViewSuccessScreen(
                    state = state,
                    lists = pagingData,
                    onListSelected = {
                        navigator.push(ListViewScreen(it.list.id, it.list.supabaseId.orEmpty()))
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        }
    }
}

@Composable
private fun ProfileViewLoadingScreen(
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
) {
    Scaffold(
        topBar = {
            ShimmerHost {
                SearchLargeTopBar(
                    title = { TextPlaceholder() },
                    actions = {
                        ButtonPlaceholder(
                            Modifier
                                .padding(12.dp)
                                .size(42.dp)
                        )
                    },
                    navigationIcon = {
                        Spacer(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.onSurface
                                        .copy(
                                            alpha = lerp(
                                                0f,
                                                1f,
                                                CubicBezierEasing(.8f, 0f, .8f, .15f).transform(
                                                    scrollBehavior.state.collapsedFraction
                                                )
                                            )
                                        )
                                )

                        )
                    },
                    colors = TopAppBarDefaults.colors2(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    extraContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(MaterialTheme.colorScheme.onSurface)
                            )
                            Spacer(modifier = Modifier.width(22.dp))
                            Column(
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                TextPlaceholder()
                                TextPlaceholder()
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            ) { paddingValues ->
            ShimmerHost {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)) {
                    repeat(8) {
                        item {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(Modifier.height(90.dp)) {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.onSurface)
                                    )
                                    Spacer(Modifier.padding(8.dp))
                                    Column(Modifier.fillMaxHeight()) {
                                        repeat(3) {
                                            TextPlaceholder(
                                                Modifier
                                                    .weight(1f)
                                                    .clip(CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }
}

@Preview
@Composable
private fun PreviewProfileViewLoadingScreen() {
    ProvideLocalsForPreviews {
        MovieTheme {
            ProfileViewLoadingScreen()
        }
    }
}

@Composable
private fun ProfileViewSuccessScreen(
    state: ProfileViewState.Success,
    lists: LazyPagingItems<ListPreviewItem>,
    onListSelected: (ListPreviewItem) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {

    var selectedList by remember {
        mutableStateOf<ListPreviewItem?>(null)
    }

    Scaffold(
        topBar = {
            ProfileTopBar(
                scrollBehavior = scrollBehavior,
                user = state.user,
                onProfileImageClicked = { },
                showOptionsClicked = {}
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        ScrollbarLazyColumn(
            contentPadding = paddingValues
        ) {
            val notLoadingAndEmpty =
                lists.loadState.append is LoadState.NotLoading
                        && lists.loadState.append is LoadState.NotLoading
                        && lists.itemCount == 0
            if (notLoadingAndEmpty) {
                item("empty_item") {
                    Box(modifier = Modifier
                        .height(300.dp)
                        .fillMaxWidth(), contentAlignment = Alignment.Center) {
                        NoResultsEmptyScreen(contentPaddingValues = PaddingValues())
                    }
                }
            }
            item("refreshing_items") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when(lists.loadState.refresh) {
                        is LoadState.Error -> {
                            TextButton(onClick = { lists.retry() }) {
                                Text(text = "Retry Loading lists")
                            }
                        }
                        LoadState.Loading -> CircularProgressIndicator()
                        is LoadState.NotLoading -> Unit
                    }
                }
            }
            items(lists.itemCount, lists.itemKey(), lists.itemContentType()) {
                val list = lists[it] ?: return@items

                ContentListPreview(
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = { selectedList = list },
                            onClick = { onListSelected(list) }
                        )
                        .animateItem()
                        .padding(8.dp),
                    cover = {
                        ContentListPosterStateFlowItems(
                            list = list.list,
                            items = list.items,
                            modifier = Modifier
                                .aspectRatio(ItemCover.Square.ratio)
                                .fillMaxSize()
                        )
                    },
                    name = list.list.name,
                    description = list.list.description
                )
            }
            item("loading_items") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when(lists.loadState.append) {
                        is LoadState.Error -> {
                            TextButton(onClick = { lists.retry() }) {
                                Text(text = "Retry Loading comments")
                            }
                        }
                        LoadState.Loading -> PageLoadingIndicator()
                        is LoadState.NotLoading -> Unit
                    }
                }
            }
        }
    }
    selectedList?.let { (list, _, _, items) ->

        val listInteractor = LocalListInteractor.current
        val navigator = LocalNavigator.currentOrThrow

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
            isUserMe = list.createdBy == state.user.userId || list.createdBy == null,
            content = remember(items) { items.map { it.value } },
            onSubscribeClicked = {
                listInteractor.subscribeToList(list)
                selectedList = null
            },
            onUnsubscribeClicked = {
                listInteractor.unsubscribeFromList(list)
                selectedList = null
            },
            onTogglePinned = {
                listInteractor.togglePinned(list)
                selectedList = null
            }
        )
    }
}
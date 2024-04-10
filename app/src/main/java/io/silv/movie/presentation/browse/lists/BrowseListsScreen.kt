package io.silv.movie.presentation.browse.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExploreOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.map
import app.cash.paging.PagingSource
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
import io.silv.core_ui.components.topbar.SearchLargeTopBar
import io.silv.core_ui.components.topbar.colors2
import io.silv.core_ui.util.rememberDominantColor
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.LocalUser
import io.silv.movie.R
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.lists.toContentItem
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.prefrences.BasePreferences
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.user.ListRepository
import io.silv.movie.data.user.ListWithItems
import io.silv.movie.presentation.library.components.ContentItemSourceCoverOnlyGridItem
import io.silv.movie.presentation.library.components.ContentListPosterItems
import io.silv.movie.presentation.library.components.topbar.PosterLargeTopBarDefaults
import io.silv.movie.presentation.library.screens.ListViewScreen
import io.silv.movie.presentation.profile.UserProfileImage
import io.silv.movie.presentation.toPoster
import io.silv.movie.presentation.view.movie.MovieViewScreen
import io.silv.movie.presentation.view.tv.TVViewScreen
import io.silv.movie.rememberProfileImageData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber

@Serializable
data class PopularListParams(
    val lim: Int,
    val off: Int,
)

@Serializable
data class ListByIdParams(
    @SerialName("list_ids")
    val listIds: String
) {

    companion object {
        fun of(list: List<String>): ListByIdParams {
            return ListByIdParams(
                listIds = "{${list.joinToString()}}"
            )
        }
    }
}


data class ListPreviewItem(
    val list: ContentList,
    val username: String,
    val profileImage: String?,
    val items: ImmutableList<ContentItem>,
)

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

class BrowseListsScreenModel(
    private val postgrest: Postgrest,
    private val contentListRepository: ContentListRepository,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
    private val basePreferences: BasePreferences,
    private val listRepository: ListRepository,
    private val auth: Auth,
): ScreenModel {

    private val popularResult = MutableStateFlow<List<ListWithPostersRpcResponse>>(emptyList())
    private val recentlyCreatedResult = MutableStateFlow<List<ListWithPostersRpcResponse>>(emptyList())
    private val recentlyViewedResult = MutableStateFlow<List<ListWithPostersRpcResponse>>(emptyList())
    private val subscribedRecommendedResult = MutableStateFlow<List<ListWithPostersRpcResponse>>(emptyList())
    private val _defaultLists = MutableStateFlow<ImmutableList<Pair<ContentList, ImmutableList<ContentItem>>>>(persistentListOf())

    private val recentIds = basePreferences.recentlyViewedLists()

    val defaultLists = _defaultLists.asStateFlow()

    val subscribedRecommended = subscribedRecommendedResult.asStateFlow()
        .map { response ->
            response.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, getShow, getMovie)
            }
                .toImmutableList()
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            persistentListOf()
        )

    val recentlyCreated= recentlyCreatedResult.asStateFlow()
        .map { response ->
            response.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, getShow, getMovie)
            }
                .toImmutableList()
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            persistentListOf()
        )

    val recentlyViewed= recentlyViewedResult.asStateFlow()
        .map { response ->
            response.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, getShow, getMovie)
            }
                .toImmutableList()
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            persistentListOf()
        )

    val popularLists = popularResult.asStateFlow()
        .map { response ->
            response.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, getShow, getMovie)
            }
                .toImmutableList()
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            persistentListOf()
        )

    init {
        refresh()
    }

    private var refreshJob: Job?  = null

    private fun refresh() {
        if (refreshJob?.isActive == true)
            return

        refreshJob = ioCoroutineScope.launch {
            try {
                supervisorScope {
                    launch {
                        popularResult.emit(
                            postgrest.rpc(
                                "select_most_popular_lists_with_poster_items",
                                PopularListParams(10, 0)
                            )
                                .decodeList<ListWithPostersRpcResponse>()
                        )
                            .also { Timber.d(it.toString()) }
                    }
                    launch {
                        recentlyCreatedResult.emit(
                            postgrest.rpc(
                                "select_most_recent_lists_with_poster_items",
                                PopularListParams(10, 0)
                            )
                                .decodeList<ListWithPostersRpcResponse>()
                        )
                            .also { Timber.d(it.toString()) }
                    }
                    launch {
                        recentlyViewedResult.emit(
                            postgrest.rpc(
                                "select_lists_by_ids_with_poster",
                                ListByIdParams.of(
                                    recentIds.get().toList()
                                )
                            )
                                .decodeList<ListWithPostersRpcResponse>()
                        )
                            .also { Timber.d(it.toString()) }
                    }
                    launch {
                        val lists = listRepository
                            .selectListsByUserId("c532e5da-71ca-4b4b-b896-d1d36f335149")
                            ?.map { listWithItems ->
                                toContentListWithItems(listWithItems)
                            }
                            .orEmpty()
                            .toImmutableList()
                        _defaultLists.emit(lists)
                    }
                    launch sub@{
                        val fromSubscribed = listRepository.selectRecommendedFromSubscriptions() ?: return@sub
                        subscribedRecommendedResult.emit(fromSubscribed)
                    }
                }
            } catch (e :Exception) {
                Timber.e(e)
            }
        }
    }

    private suspend fun toContentListWithItems(listWithItems: ListWithItems): Pair<ContentList, ImmutableList<ContentItem>> {
        val local = contentListRepository.getListForSupabaseId(listWithItems.listId)
        val list = local ?: ContentList(
            id = -1,
            supabaseId = listWithItems.listId,
            createdBy = "c532e5da-71ca-4b4b-b896-d1d36f335149",
            lastSynced = null,
            public = true,
            name = listWithItems.name,
            username = "Default User",
            description = listWithItems.description,
            lastModified = -1L,
            posterLastModified = -1L,
            createdAt = -1L,
            inLibrary = false
        )
        val posters = listWithItems.items.orEmpty().map {
            val isMovie = it.movieId != -1L
            val id = it.movieId.takeIf { isMovie } ?: it.showId

            val item = if(isMovie)
                getMovie.await(id)?.toContentItem()
            else
                getShow.await(id)?.toContentItem()

            item ?: ContentItem(
                contentId = id,
                isMovie = isMovie,
                title = "",
                posterUrl = "https://image.tmdb.org/t/p/original/${it.posterPath}",
                favorite = false,
                inLibraryLists = -1L,
                posterLastUpdated = -1L,
                lastModified = -1L,
                description = "",
                popularity = -1.0
            )
        }
        return Pair(list, posters.toImmutableList())
    }
}


data object BrowseListsScreen: Screen {

    @Composable
    override fun Content(){

        val screenModel = getScreenModel<BrowseListsScreenModel>()
        val lists by screenModel.popularLists.collectAsStateWithLifecycle()
        val subscribedRecommended by  screenModel.subscribedRecommended.collectAsStateWithLifecycle()
        val recentlyCreated by  screenModel.recentlyCreated.collectAsStateWithLifecycle()
        val recentlyViewed by screenModel.recentlyViewed.collectAsStateWithLifecycle()
        val default by screenModel.defaultLists.collectAsStateWithLifecycle()

        val navigator = LocalNavigator.currentOrThrow
        val user = LocalUser.current
        val profileImageData = user.rememberProfileImageData()

        val hazeState = remember { HazeState() }

        val dominantColor by rememberDominantColor(data = profileImageData)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (user != null) {
                            UserProfileImage(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(36.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                dominantColor,
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .aspectRatio(1f),
                                contentDescription = user.username
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(Color.Transparent),
                    title = {
                        user?.let { Text(it.username, style = MaterialTheme.typography.labelLarge) }
                    },
                    actions = {
                        IconButton(onClick = { navigator.push(SearchForListScreen) }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        }
                    },
                    modifier = Modifier.hazeChild(hazeState)
                )
            }
        ) { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxSize()
                    .haze(hazeState, HazeDefaults.style(MaterialTheme.colorScheme.background)),
            ) {
                item {
                    FlowRow(
                        maxItemsInEachRow = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .wrapContentHeight()
                    ) {
                        recentlyViewed.forEachIndexed { i, it ->
                            RecentlyViewedPreview(
                                modifier = Modifier
                                    .padding(vertical = 4.dp,)
                                    .padding(
                                        start = if (i % 2 == 0) 0.dp else 4.dp,
                                        end = if (i % 2 == 0) 4.dp else 0.dp
                                    )
                                    .clickable {
                                        navigator.push(
                                            ListViewScreen(
                                                it.list.id,
                                                it.list.supabaseId.orEmpty()
                                            )
                                        )
                                    }
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(Color.DarkGray),
                                cover = {
                                    ContentListPosterItems(
                                        list = it.list,
                                        items = it.items,
                                    )
                                },
                                name = it.list.name,
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Most popular",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .padding(vertical = 22.dp)
                        )
                        TextButton(
                            onClick = { }
                        ) {
                            Text(text = "View more")
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                item {
                    LazyRow {
                        item { Spacer(modifier = Modifier.width(12.dp)) }
                        items(
                            items = lists, key = { it.list.supabaseId.orEmpty() + it.list.id }
                        ) {
                            RowPreviewItem(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable {
                                        navigator.push(
                                            ListViewScreen(
                                                it.list.id,
                                                it.list.supabaseId.orEmpty()
                                            )
                                        )
                                    },
                                cover = {
                                    ContentListPosterItems(
                                        list = it.list,
                                        items = it.items,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                    )
                                },
                                name = it.list.name,
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "More from subscribed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .padding(vertical = 22.dp)
                        )
                        TextButton(
                            onClick = { }
                        ) {
                            Text(text = "View more")
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                item {
                    LazyRow {
                        item { Spacer(modifier = Modifier.width(12.dp)) }
                        items(
                            items = subscribedRecommended, key = { it.list.supabaseId.orEmpty() + it.list.id }
                        ) {
                            RowPreviewItem(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable {
                                        navigator.push(
                                            ListViewScreen(
                                                it.list.id,
                                                it.list.supabaseId.orEmpty()
                                            )
                                        )
                                    },
                                cover = {
                                    ContentListPosterItems(
                                        list = it.list,
                                        items = it.items,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                    )
                                },
                                name = it.list.name,
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Recently created",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .padding(vertical = 22.dp)
                        )
                        TextButton(
                            onClick = { }
                        ) {
                            Text(text = "View more")
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                item {
                    LazyRow {
                        item { Spacer(modifier = Modifier.width(12.dp)) }
                        items(
                            items = recentlyCreated, key = { it.list.supabaseId.orEmpty() + it.list.id }
                        ) {
                            RowPreviewItem(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable {
                                        navigator.push(
                                            ListViewScreen(
                                                it.list.id,
                                                it.list.supabaseId.orEmpty()
                                            )
                                        )
                                    },
                                cover = {
                                    ContentListPosterItems(
                                        list = it.list,
                                        items = it.items,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                    )
                                },
                                name = it.list.name,
                            )
                        }
                    }
                }
                default.fastForEach { (list, items) ->
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = list.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .padding(vertical = 22.dp)
                            )
                            TextButton(
                                onClick = { navigator.push(ListViewScreen(list.id, list.supabaseId.orEmpty())) }
                            ) {
                                Text(text = "View list")
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    item {
                        LazyRow {
                            item { Spacer(modifier = Modifier.width(12.dp)) }
                            items(
                                items = items,
                            ) {
                                Box(
                                    modifier = Modifier.height(128.dp)
                                ) {
                                    ContentItemSourceCoverOnlyGridItem(
                                        favorite = it.favorite,
                                        poster = remember(it) { it.toPoster() },
                                        onClick = {
                                            if (it.isMovie) {
                                                navigator.push(MovieViewScreen(it.contentId))
                                            } else {
                                                navigator.push(TVViewScreen(it.contentId))
                                            }
                                        },
                                        onLongClick = {

                                        },
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


@Composable
fun RowPreviewItem(
    cover: @Composable () -> Unit,
    name: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .fillMaxWidth()
        ) {
            cover()
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = name,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun RecentlyViewedPreview(
    cover: @Composable () -> Unit,
    name: String,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxHeight()
        ) {
            cover()
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f, true)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = name,
                maxLines = 2,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

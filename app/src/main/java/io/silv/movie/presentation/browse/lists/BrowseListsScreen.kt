package io.silv.movie.presentation.browse.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExploreOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc
import io.silv.core_ui.components.PullRefresh
import io.silv.core_ui.components.shimmer.ShimmerHost
import io.silv.core_ui.components.shimmer.TextPlaceholder
import io.silv.core_ui.util.rememberDominantColor
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
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
import io.silv.movie.presentation.LocalListInteractor
import io.silv.movie.presentation.library.components.ContentItemSourceCoverOnlyGridItem
import io.silv.movie.presentation.library.components.ContentListPoster
import io.silv.movie.presentation.library.components.dialog.ListOptionsBottomSheet
import io.silv.movie.presentation.library.screens.ListAddScreen
import io.silv.movie.presentation.library.screens.ListEditDescriptionScreen
import io.silv.movie.presentation.library.screens.ListEditScreen
import io.silv.movie.presentation.library.screens.ListViewScreen
import io.silv.movie.presentation.profile.ProfileTab
import io.silv.movie.presentation.profile.UserProfileImage
import io.silv.movie.presentation.toPoster
import io.silv.movie.presentation.view.movie.MovieViewScreen
import io.silv.movie.presentation.view.tv.TVViewScreen
import io.silv.movie.rememberProfileImageData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
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


class BrowseListsScreenModel(
    private val postgrest: Postgrest,
    private val contentListRepository: ContentListRepository,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
    private val listRepository: ListRepository,
    basePreferences: BasePreferences,
    auth: Auth,
): ScreenModel {

    private val DEFAULT_LIST_USER = "c532e5da-71ca-4b4b-b896-d1d36f335149"
    var refreshing by mutableStateOf(false)
        private set

    private val jobs = listOf(
        suspend {
            popularResult.emit(
                runCatching {
                    postgrest.rpc(
                        "select_most_popular_lists_with_poster_items",
                        PopularListParams(10, 0)
                    )
                        .decodeList<ListWithPostersRpcResponse>()
                }.getOrDefault(emptyList())
            )
        },
        suspend {
            recentlyCreatedResult.emit(
                runCatching {
                    postgrest.rpc(
                        "select_most_recent_lists_with_poster_items",
                        PopularListParams(10, 0)
                    )
                        .decodeList<ListWithPostersRpcResponse>()
                }.getOrDefault(emptyList())
            )
        },
        suspend {
            recentlyViewedResult.emit(
                runCatching {
                    postgrest.rpc(
                        "select_lists_by_ids_with_poster",
                        ListByIdParams.of(
                            recentIds.get().toList()
                        )
                    )
                        .decodeList<ListWithPostersRpcResponse>()
                }.getOrDefault(emptyList())
            )
        },
        suspend {
            val lists = runCatching {
                listRepository
                    .selectListsByUserId(DEFAULT_LIST_USER)
                    ?.map { listWithItems ->
                        toContentListWithItems(listWithItems)
                    }
                    .orEmpty()
                    .toImmutableList()
            }
                .getOrDefault(persistentListOf())

            _defaultLists.emit(lists)
        },
        suspend sub@{
            val fromSubscribed = listRepository.selectRecommendedFromSubscriptions() ?: return@sub
            subscribedRecommendedResult.emit(fromSubscribed)
        }
    )

    private val popularResult =
        MutableStateFlow<List<ListWithPostersRpcResponse>?>(null)
    private val recentlyCreatedResult =
        MutableStateFlow<List<ListWithPostersRpcResponse>?>(null)
    private val recentlyViewedResult =
        MutableStateFlow<List<ListWithPostersRpcResponse>?>(null)
    private val subscribedRecommendedResult =
        MutableStateFlow<List<ListWithPostersRpcResponse>?>(null)
    private val _defaultLists =
        MutableStateFlow<ImmutableList<Pair<ContentList, ImmutableList<ContentItem>>>?>(null)

    private val recentIds = basePreferences.recentlyViewedLists()

    val defaultLists = _defaultLists.asStateFlow()

    val subscribedRecommended = subscribedRecommendedResult.asStateFlow()
        .map { response ->
            response?.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, getShow, getMovie)
            }
                ?.toImmutableList()
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    val recentlyCreated= recentlyCreatedResult.asStateFlow()
        .map { response ->
            response?.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, getShow, getMovie)
            }
                ?.toImmutableList()
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    val recentlyViewed= recentlyViewedResult.asStateFlow()
        .map { response ->
            response?.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, getShow, getMovie)
            }
                ?.toImmutableList()
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    val popularLists = popularResult.asStateFlow()
        .map { response ->
            response?.map { listWithPosters ->
                listWithPosters.toListPreviewItem(contentListRepository, getShow, getMovie)
            }
                ?.toImmutableList()
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    init {
        refresh()

        basePreferences.recentlyViewedLists()
            .changes()
            .drop(1)
            .onEach {
                jobs[2].invoke()
            }
            .launchIn(screenModelScope)

        auth.sessionStatus
            .drop(1)
            .onEach {
                if (it !is SessionStatus.Authenticated) {
                    subscribedRecommendedResult.emit(persistentListOf())
                } else {
                    jobs.last().invoke()
                }
            }
            .launchIn(screenModelScope)
    }

    private var refreshJob: Job?  = null

    fun refresh(isUserAction: Boolean = false) {
        if (refreshJob?.isActive == true)
            return

        if (isUserAction) {
            refreshing = true
        }

        refreshJob = ioCoroutineScope.launch {
            supervisorScope {
                val running = jobs.map {
                    launch {
                        runCatching { it() }.onFailure { Timber.e(it) }
                    }
                }

                running.joinAll()

                withContext(Dispatchers.Main) { refreshing = false }
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
            createdAt = listWithItems.createdAt.toEpochMilliseconds(),
            inLibrary = false,
            subscribers = listWithItems.subscribers
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
    override fun Content() {

        val screenModel = getScreenModel<BrowseListsScreenModel>()
        val popularUserLists by screenModel.popularLists.collectAsStateWithLifecycle()
        val subscribedRecommended by screenModel.subscribedRecommended.collectAsStateWithLifecycle()
        val recentlyCreated by screenModel.recentlyCreated.collectAsStateWithLifecycle()
        val recentlyViewed by screenModel.recentlyViewed.collectAsStateWithLifecycle()
        val default by screenModel.defaultLists.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.currentOrThrow
        val tabNavigator = LocalTabNavigator.current
        val listInteractor = LocalListInteractor.current
        val user = LocalUser.current
        val profileImageData = user.rememberProfileImageData()
        val hazeState = remember { HazeState() }
        val dominantColor by rememberDominantColor(data = profileImageData)

        val navigateOnListClick = { item: ListPreviewItem ->
            navigator.push(ListViewScreen(item.list.id, item.list.supabaseId.orEmpty()))
        }

        var selectedList by remember {
            mutableStateOf<Pair<ContentList, ImmutableList<ContentItem>>?>(null)
        }

        PullRefresh(
            refreshing = screenModel.refreshing,
            enabled = { true },
            onRefresh = { screenModel.refresh(true) }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                UserProfileImage(
                                    modifier = Modifier
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
                                    contentDescription = user?.username.orEmpty()
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            dominantColor.copy(alpha = 0.1f)
                        ),
                        title = {
                            TextButton(
                                onClick = {
                                    tabNavigator.current = ProfileTab
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = dominantColor
                                )
                            ) {
                                Text(user?.username ?: "Sign in")
                            }
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
                    recentlyViewedListsPreview(
                        recentlyViewed,
                        onListClick = navigateOnListClick,
                        onListLongClick = { selectedList = it.list to it.items }
                    )
                    listCategoryPreview(
                        label = {
                            TitleWithAction(
                                title = stringResource(id = R.string.most_popular_lists),
                                actionLabel = stringResource(id = R.string.view_more),
                                onAction = { }
                            )
                        },
                        empty = {
                            Text(
                                stringResource(
                                    R.string.no_items_for,
                                    stringResource(id = R.string.most_popular_lists),
                                    "most popular public user lists will appear here"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .align(Alignment.Center)
                            )
                        },
                        lists = popularUserLists,
                        onListClick = navigateOnListClick,
                        onListLongClick = { selectedList = it.list to it.items },
                        tag = "popular_lists",
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (user != null) {
                        listCategoryPreview(
                            label = {
                                TitleWithAction(
                                    title = stringResource(id = R.string.more_from_subscribed),
                                    actionLabel = stringResource(id = R.string.view_more),
                                    onAction = { }
                                )
                            },
                            empty = {
                                Text(
                                    stringResource(
                                        R.string.no_items_for,
                                        stringResource(id = R.string.more_from_subscribed),
                                        "subscribe to lists to see more"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                )
                            },
                            lists = subscribedRecommended,
                            onListClick = navigateOnListClick,
                            onListLongClick = { selectedList = it.list to it.items },
                            tag = "subscribed_recommended",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    listCategoryPreview(
                        label = {
                            TitleWithAction(
                                title = stringResource(id = R.string.recently_created),
                                actionLabel = stringResource(id = R.string.view_more),
                                onAction = { }
                            )
                        },
                        empty = {
                            Text(
                                stringResource(
                                    R.string.no_items_for,
                                    stringResource(id = R.string.recently_created),
                                    "all users public created lists will appear here"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .align(Alignment.Center)
                            )
                        },
                        lists = recentlyCreated,
                        onListClick = navigateOnListClick,
                        onListLongClick = { selectedList = it.list to it.items },
                        tag = "recently_created",
                        modifier = Modifier.fillMaxWidth()
                    )
                    defaultListsPreview(
                        default,
                        onListClick = {
                            navigator.push(ListViewScreen(it.id, it.supabaseId.orEmpty()))
                        },
                        onItemClick = {
                            navigator.push(
                                if (it.isMovie)
                                    MovieViewScreen(it.contentId)
                                else
                                    TVViewScreen(it.contentId)
                            )
                        },
                        onItemLongClick = {

                        }
                    )
                    item {
                        Spacer(Modifier.height(22.dp))
                    }
                }
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
                isUserMe = list.createdBy == user?.userId || list.createdBy == null,
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


@Composable
fun TitleWithAction(
    title: String,
    actionLabel: String,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 12.dp)
                .padding(vertical = 22.dp)
        )
        if (onAction != null) {
            TextButton(
                onClick = onAction
            ) {
                Text(text = actionLabel)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


fun LazyListScope.listCategoryPreview(
    label: @Composable () -> Unit,
    empty: @Composable BoxScope.() -> Unit,
    lists: ImmutableList<ListPreviewItem>?,
    onListClick: (ListPreviewItem) -> Unit,
    onListLongClick: (ListPreviewItem) -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    item("$tag-label") {
        label()
    }
    lists?.let {
        item(tag) {
            if (it.isEmpty()) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(12.dp)
                ) {
                    empty()
                }
            } else {
                LazyRow(
                    modifier = modifier
                ) {
                    item { Spacer(modifier = Modifier.width(12.dp)) }
                    items(
                        items = it,
                        key = { it.list.supabaseId.orEmpty() + it.list.id }
                    ) {
                        RowPreviewItem(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(MaterialTheme.shapes.small)
                                .combinedClickable(
                                    onLongClick = { onListLongClick(it) },
                                    onClick = { onListClick(it) }
                                )
                                .padding(4.dp),
                            cover = {
                                ContentListPoster(
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
        }
    } ?: item("$tag-placeholder") {
        ShimmerHost {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .horizontalScroll(
                        rememberScrollState(),
                        enabled = false
                    )
                    .fillMaxWidth()
            ) {
                repeat(4) {
                    PosterShimmerItem()
                }
            }
        }
    }
}



@Composable
fun PosterShimmerItem() {
    Column(
        Modifier
            .size(120.dp)
            .padding(horizontal = 12.dp)
    ) {
        Spacer(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.onSurface)
                .clipToBounds()
        )
        TextPlaceholder()
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

fun LazyListScope.defaultListsPreview(
    defaultLists: ImmutableList<Pair<ContentList, ImmutableList<ContentItem>>>?,
    onListClick: (ContentList) -> Unit,
    onItemClick: (ContentItem) -> Unit,
    onItemLongClick: () -> Unit,
) {
    defaultLists?.fastForEach {(list, items) ->
        item {
            Column {
                TitleWithAction(
                    title = list.name,
                    actionLabel = stringResource(id = R.string.view_more),
                    onAction = {
                        onListClick(list)
                    }
                )
                LazyRow {
                    items(items) {
                        Box(Modifier.height(128.dp)) {
                            ContentItemSourceCoverOnlyGridItem(
                                favorite = it.favorite,
                                poster = remember(it) { it.toPoster() },
                                onClick = { onItemClick(it) },
                                onLongClick = onItemLongClick,
                            )
                        }
                    }
                }
            }
        }
    } ?: item {
        ShimmerHost {
            Column {
                repeat(2) {
                    Spacer(Modifier.padding(6.dp))
                    TextPlaceholder(
                        Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(Modifier.padding(6.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState(), false)
                    ) {
                        repeat(10) {
                            Spacer(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .width(64.dp)
                                    .aspectRatio(2 / 3f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                }
            }
        }
    }
}


fun LazyListScope.recentlyViewedListsPreview(
    lists: ImmutableList<ListPreviewItem>?,
    onListClick: (ListPreviewItem) -> Unit,
    onListLongClick: (ListPreviewItem) -> Unit,
) {
    lists?.let { recents ->
        item(
            "recent-items"
        ) {
            if (recents.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { alpha = 0.78f }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExploreOff,
                            contentDescription = null,
                            Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.recently_viewed_empty),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                FlowRow(
                    maxItemsInEachRow = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .wrapContentHeight()
                ) {
                    recents.forEachIndexed { i, it ->
                        RecentlyViewedPreview(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .padding(
                                    start = if (i and 1 == 0) 0.dp else 4.dp,
                                    end = if (i and 1 == 0) 4.dp else 0.dp
                                )
                                .combinedClickable(
                                    onLongClick = { onListLongClick(it) }
                                ) { onListClick(it) }
                                .weight(1f)
                                .height(64.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                            cover = {
                                ContentListPoster(
                                    list = it.list,
                                    items = it.items,
                                )
                            },
                            name = it.list.name,
                        )
                    }
                }
            }
        }
    } ?: item(
        "recent-placeholder"
    ) {
        ShimmerHost {
            Column(
                Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
                repeat(3) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Spacer(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .padding(
                                    start = 0.dp,
                                    end = 4.dp
                                )
                                .height(64.dp)
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.onSurface)
                        )
                        Spacer(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .padding(
                                    start = 4.dp,
                                    end = 0.dp
                                )
                                .height(64.dp)
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }
            }
        }
    }
}
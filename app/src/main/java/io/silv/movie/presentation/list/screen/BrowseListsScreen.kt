package io.silv.movie.presentation.list.screen

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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.PullRefresh
import io.silv.core_ui.components.shimmer.ShimmerHost
import io.silv.core_ui.components.shimmer.TextPlaceholder
import io.silv.core_ui.util.rememberDominantColor
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
import io.silv.movie.R
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.presentation.LocalListInteractor
import io.silv.movie.presentation.LocalUser
import io.silv.movie.presentation.components.content.ContentItemSourceCoverOnlyGridItem
import io.silv.movie.presentation.components.content.ContentListPosterStateFlowItems
import io.silv.movie.presentation.components.dialog.ListOptionsBottomSheet
import io.silv.movie.presentation.content.screen.MovieViewScreen
import io.silv.movie.presentation.content.screen.TVViewScreen
import io.silv.movie.presentation.list.screenmodel.BrowseListsScreenModel
import io.silv.movie.presentation.list.screenmodel.ListPagedType
import io.silv.movie.presentation.list.screenmodel.ListPreviewItem
import io.silv.movie.presentation.profile.UserProfileImage
import io.silv.movie.presentation.rememberProfileImageData
import io.silv.movie.presentation.result.screen.ListAddScreen
import io.silv.movie.presentation.result.screen.ListEditDescriptionScreen
import io.silv.movie.presentation.result.screen.ListEditScreen
import io.silv.movie.presentation.tabs.ProfileTab
import io.silv.movie.presentation.toPoster
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow


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
                val selectList = { item: ListPreviewItem ->
                    selectedList = item.list to item.items.mapNotNull { it.value }.toImmutableList()
                }
                LazyColumn(
                    contentPadding = paddingValues,
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(hazeState, HazeDefaults.style(MaterialTheme.colorScheme.background)),
                ) {
                    recentlyViewedListsPreview(
                        recentlyViewed,
                        onListClick = navigateOnListClick,
                        onListLongClick = {

                        }
                    )
                    listCategoryPreview(
                        label = {
                            TitleWithAction(
                                title = stringResource(id = R.string.most_popular_lists),
                                actionLabel = stringResource(id = R.string.view_more),
                                onAction = { navigator.push(PagedListScreen(ListPagedType.Popular)) }
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
                        onListLongClick = selectList,
                        tag = "popular_lists",
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (user != null) {
                        listCategoryPreview(
                            label = {
                                TitleWithAction(
                                    title = stringResource(id = R.string.more_from_subscribed),
                                    actionLabel = stringResource(id = R.string.view_more),
                                    onAction = { navigator.push(PagedListScreen(ListPagedType.MoreFromSubscribed)) }
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
                            onListLongClick = selectList,
                            tag = "subscribed_recommended",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    listCategoryPreview(
                        label = {
                            TitleWithAction(
                                title = stringResource(id = R.string.recently_created),
                                actionLabel = stringResource(id = R.string.view_more),
                                onAction = { navigator.push(PagedListScreen(ListPagedType.Recent)) }
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
                        onListLongClick = selectList,
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
                },
                onTogglePinned = {
                    listInteractor.togglePinned(list)
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
                                val items =

                                ContentListPosterStateFlowItems(
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
    defaultLists: ImmutableList<Pair<ContentList, ImmutableList<StateFlow<ContentItem>>>>?,
    onListClick: (ContentList) -> Unit,
    onItemClick: (ContentItem) -> Unit,
    onItemLongClick: () -> Unit,
) {
    defaultLists?.fastForEach { (list, items) ->
        item {
            Column {
                TitleWithAction(
                    title = list.name,
                    actionLabel = stringResource(id = R.string.view_list),
                    onAction = {
                        onListClick(list)
                    }
                )
                LazyRow {
                    items(items) {
                        val item by it.collectAsStateWithLifecycle()
                        Box(Modifier.height(128.dp)) {
                            ContentItemSourceCoverOnlyGridItem(
                                favorite = item.favorite,
                                poster = remember(item) { item.toPoster() },
                                onClick = { onItemClick(item) },
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
                                    start = if (i and 1 == 0 || i == recents.lastIndex) 0.dp else 4.dp,
                                    end = if (i and 1 == 0 && i != recents.lastIndex) 4.dp else 0.dp
                                )
                                .combinedClickable(
                                    onLongClick = { onListLongClick(it) }
                                ) { onListClick(it) }
                                .weight(1f)
                                .height(64.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                            cover = {
                                ContentListPosterStateFlowItems(
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
    } ?: item("recent-placeholder") {
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
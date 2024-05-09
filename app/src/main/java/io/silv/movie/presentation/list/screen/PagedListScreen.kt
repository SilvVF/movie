package io.silv.movie.presentation.list.screen

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
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.Action
import io.silv.core_ui.components.EmptyScreen
import io.silv.core_ui.components.NoResultsEmptyScreen
import io.silv.core_ui.components.loadingIndicatorItem
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
import io.silv.movie.R
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.presentation.LocalListInteractor
import io.silv.movie.presentation.LocalUser
import io.silv.movie.presentation.components.content.ContentListPosterStateFlowItems
import io.silv.movie.presentation.components.dialog.ListOptionsBottomSheet
import io.silv.movie.presentation.list.screenmodel.ListPagedScreenModel
import io.silv.movie.presentation.list.screenmodel.ListPagedType
import io.silv.movie.presentation.list.screenmodel.ListPreviewItem
import io.silv.movie.presentation.result.screen.ListAddScreen
import io.silv.movie.presentation.result.screen.ListEditDescriptionScreen
import io.silv.movie.presentation.result.screen.ListEditScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.core.parameter.parametersOf


data class PagedListScreen(
    val pagedType: ListPagedType
): Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<ListPagedScreenModel> { parametersOf(pagedType) }
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
                },
                onTogglePinned = {
                    listInteractor.togglePinned(list)
                    selectedList = null
                }
            )
        }
    }
}
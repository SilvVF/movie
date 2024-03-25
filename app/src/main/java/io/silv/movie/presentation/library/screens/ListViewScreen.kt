package io.silv.movie.presentation.library.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExploreOff
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.Action
import io.silv.core_ui.components.EmptyScreen
import io.silv.core_ui.components.PullRefresh
import io.silv.core_ui.components.topbar.rememberPosterTopBarState
import io.silv.core_ui.util.rememberDominantColor
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
import io.silv.movie.R
import io.silv.movie.data.cache.ListCoverCache
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.CollectEventsWithLifecycle
import io.silv.movie.presentation.browse.components.RemoveEntryDialog
import io.silv.movie.presentation.library.components.ContentListPosterGrid
import io.silv.movie.presentation.library.components.ContentListPosterList
import io.silv.movie.presentation.library.components.dialog.ListOptionsBottomSheet
import io.silv.movie.presentation.library.components.dialog.ListViewCoverDialog
import io.silv.movie.presentation.library.components.topbar.ListViewTopBar
import io.silv.movie.presentation.library.screenmodels.ListCoverScreenModel
import io.silv.movie.presentation.library.screenmodels.ListSortMode
import io.silv.movie.presentation.library.screenmodels.ListViewEvent
import io.silv.movie.presentation.library.screenmodels.ListViewScreenModel
import io.silv.movie.presentation.library.screenmodels.ListViewState
import io.silv.movie.presentation.toPoster
import io.silv.movie.presentation.view.components.EditCoverAction
import io.silv.movie.presentation.view.movie.MovieViewScreen
import io.silv.movie.presentation.view.tv.TVViewScreen
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

data class ListViewScreen(
    private val listId: Long = -1L,
    private val supabaseId: String = ""
): Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<ListViewScreenModel> { parametersOf(listId, supabaseId) }
        val state by screenModel.state.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.currentOrThrow
        val refreshingList by screenModel.refreshingList.collectAsStateWithLifecycle()

        CollectEventsWithLifecycle(screenModel) { event ->
            when (event) {
                ListViewEvent.ListDeleted -> navigator.pop()
            }
        }

        when (val s = state) {
            is ListViewState.Error -> {
                EmptyScreen(
                    icon = Icons.Filled.ExploreOff,
                    iconSize = 182.dp,
                    actions = persistentListOf(
                        Action(
                            R.string.retry,
                            onClick = screenModel::initializeList
                        )
                    )
                )
            }
            ListViewState.Loading -> Unit
            is ListViewState.Success -> {

                val listEditScreen = remember(s.list.name) { ListEditScreen(s.list.name) }

                val screenResultLauncher = rememberScreenWithResultLauncher(
                    screen = listEditScreen
                ) { result ->
                    screenModel.editList(s.list, result.name)
                }

                val descriptionEditScreen =
                    remember(s.list.description) { ListEditDescriptionScreen(s.list.description) }

                val descriptionResultLauncher = rememberScreenWithResultLauncher(
                    screen = descriptionEditScreen
                ) { result ->
                    screenModel.editDescription(result.description)
                }

                val changeDialog = remember {
                    { sheet: ListViewScreenModel.Dialog? -> screenModel.changeDialog(sheet) }
                }
                val toggleItemFavorite = remember {
                    { contentItem: ContentItem -> screenModel.toggleItemFavorite(contentItem) }
                }


                val cache= koinInject<ListCoverCache>()
                var semaphor by remember { mutableIntStateOf(0) }
                val file = remember(semaphor) { cache.getCustomCoverFile(s.list.id) }

                LaunchedEffect(s.list.posterLastModified) {
                    semaphor++
                }

                val primary by rememberDominantColor(
                    data = when  {
                        file.exists() -> file.toUri()
                        s.allItems.isEmpty() -> null
                        else -> s.allItems.first().toPoster()
                    }
                )

                CompositionLocalProvider(
                    LocalContentColor provides primary,
                    LocalRippleTheme provides object : RippleTheme{
                        @Composable
                        override fun defaultColor(): Color = Color(
                            ColorUtils.blendARGB(
                                Color.White.toArgb(),
                                primary.toArgb(),
                                .9f
                            )
                        )
                        @Composable
                        override fun rippleAlpha(): RippleAlpha {
                            return RippleAlpha(
                                draggedAlpha = 0.9f,
                                focusedAlpha = 0.9f,
                                hoveredAlpha = 0.9f,
                                pressedAlpha = 0.9f,
                            )
                        }
                    },
                    LocalTextSelectionColors provides TextSelectionColors(
                        handleColor = primary.copy(alpha = 0.6f),
                        backgroundColor = MaterialTheme.colorScheme.background
                    )
                ) {
                SuccessScreenContent(
                    query = screenModel.query,
                    refreshingList = refreshingList,
                    refreshList = screenModel::refreshList,
                    updateQuery = screenModel::updateQuery,
                    onListOptionClick = { changeDialog(ListViewScreenModel.Dialog.ListOptions) },
                    updateListViewDisplayMode = screenModel::updateListViewDisplayMode,
                    listViewDisplayMode = { screenModel.listViewDisplayMode },
                    onClick = { item ->
                        if (item.isMovie)
                            navigator.push(MovieViewScreen(item.contentId))
                        else
                            navigator.push(TVViewScreen(item.contentId))
                    },
                    onLongClick = { item ->
                        if (item.favorite) {
                            changeDialog(ListViewScreenModel.Dialog.RemoveFromFavorites(item))
                        } else {
                            toggleItemFavorite(item)
                        }
                    },
                    onOptionsClick = { changeDialog(ListViewScreenModel.Dialog.ContentOptions(it)) },
                    updateDialog = { changeDialog(it) },
                    changeSortMode = screenModel::updateSortMode,
                    refreshRecommendations = screenModel::refreshRecommendations,
                    onAddRecommendation = screenModel::addToList,
                    onRecommendationLongClick = { item ->
                        if (item.favorite) {
                            changeDialog(ListViewScreenModel.Dialog.RemoveFromFavorites(item))
                        } else {
                            toggleItemFavorite(item)
                        }
                    },
                    onRecommendationClick = { item ->
                        if (item.isMovie)
                            navigator.push(MovieViewScreen(item.contentId))
                        else
                            navigator.push(TVViewScreen(item.contentId))
                    },
                    startAddingClick = {
                        navigator.push(
                            ListAddScreen(s.list.id)
                        )
                    },
                    primary = { primary },
                    onPosterClick = { changeDialog(ListViewScreenModel.Dialog.FullCover) },
                    state = s
                )

                val onDismissRequest = remember { { screenModel.changeDialog(null) } }
                when (val dialog = s.dialog) {
                    is ListViewScreenModel.Dialog.DeleteList -> {
                        RemoveEntryDialog(
                            onDismissRequest = onDismissRequest,
                            onConfirm = screenModel::deleteList,
                            entryToRemove = s.list.name
                        )
                    }
                    is ListViewScreenModel.Dialog.ContentOptions -> {
                        ListOptionsBottomSheet(
                            onDismissRequest = onDismissRequest,
                            onAddToAnotherListClick = {},
                            onToggleFavoriteClicked = {
                                screenModel.toggleItemFavorite(dialog.item)
                            },
                            onRemoveFromListClicked = {
                                screenModel.removeFromList(dialog.item)
                            },
                            isOwnerMe = s.isOwnerMe,
                            item = dialog.item
                        )
                    }
                    ListViewScreenModel.Dialog.ListOptions -> {
                        ListOptionsBottomSheet(
                            onDismissRequest = onDismissRequest,
                            onAddClick = { navigator.push(ListAddScreen(s.list.id)) },
                            onEditClick = { screenResultLauncher.launch() },
                            onDeleteClick = { changeDialog(ListViewScreenModel.Dialog.DeleteList) },
                            onShareClick = {},
                            list = s.list,
                            onChangeDescription = { descriptionResultLauncher.launch() },
                            content = s.allItems
                        )
                    }
                    is ListViewScreenModel.Dialog.RemoveFromFavorites -> {
                        RemoveEntryDialog(
                            onDismissRequest = onDismissRequest,
                            onConfirm = { toggleItemFavorite(dialog.item) },
                            entryToRemove = dialog.item.title
                        )
                    }
                    ListViewScreenModel.Dialog.FullCover -> {
                        val sm = getScreenModel<ListCoverScreenModel> { parametersOf(s.list.id) }
                        val list by sm.state.collectAsStateWithLifecycle()
                        val context = LocalContext.current

                        LaunchedEffect(s.list.id) {
                            sm.refresh(s.list.id)
                        }

                        if (list != null) {
                            val getContent = rememberLauncherForActivityResult(
                                ActivityResultContracts.GetContent(),
                            ) {
                                if (it == null) return@rememberLauncherForActivityResult
                                sm.editCover(context, it)
                            }
                            ListViewCoverDialog(
                                items = s.allItems,
                                list = list!!,
                                isCustomCover = remember(list) { sm.hasCustomCover(list!!) },
                                onShareClick = { sm.shareCover(context) },
                                onSaveClick = { sm.saveCover(context) },
                                snackbarHostState = sm.snackbarHostState,
                                onEditClick = {
                                    when (it) {
                                        EditCoverAction.EDIT -> getContent.launch("image/*")
                                        EditCoverAction.DELETE -> sm.deleteCustomCover(context)
                                    }
                                },
                                onDismissRequest = onDismissRequest,
                            )
                        }
                    }
                    null -> Unit
                }
            }
        }
    }
}


@Composable
private fun SuccessScreenContent(
    updateQuery: (String) -> Unit,
    onListOptionClick:() -> Unit,
    listViewDisplayMode: () -> PosterDisplayMode,
    updateListViewDisplayMode: (PosterDisplayMode) -> Unit,
    query: String,
    onLongClick: (item: ContentItem) -> Unit,
    onClick: (item: ContentItem) -> Unit,
    onOptionsClick: (item: ContentItem) -> Unit,
    changeSortMode: (ListSortMode) -> Unit,
    updateDialog: (ListViewScreenModel.Dialog?) -> Unit,
    onRecommendationClick: (item: ContentItem) -> Unit,
    onRecommendationLongClick: (item: ContentItem) -> Unit,
    onAddRecommendation: (item: ContentItem) -> Unit,
    refreshRecommendations: () -> Unit,
    startAddingClick: () -> Unit,
    onPosterClick: () -> Unit,
    refreshList: () -> Unit,
    refreshingList: Boolean,
    primary: () -> Color,
    state: ListViewState.Success
) {
    val topBarState = rememberPosterTopBarState()
    val hazeState = remember { HazeState() }
        PullRefresh(
            refreshing = refreshingList,
            enabled = { !refreshingList && state.list.supabaseId != null },
            onRefresh = refreshList
        ) {
            Scaffold(
                topBar = {
                    ListViewTopBar(
                        state  = topBarState,
                        username = state.user?.username.orEmpty(),
                        description = state.list.description,
                        userId = state.list.createdBy,
                        query = { query },
                        changeQuery = updateQuery,
                        onSearch = updateQuery,
                        items = { state.allItems },
                        contentListProvider = { state.list },
                        displayMode = listViewDisplayMode,
                        setDisplayMode = updateListViewDisplayMode,
                        onListOptionClicked = onListOptionClick,
                        sortModeProvider = { state.sortMode },
                        changeSortMode = changeSortMode,
                        onPosterClick = onPosterClick,
                        primary = primary(),
                        modifier = Modifier.hazeChild(hazeState)
                    )
                },
                modifier = Modifier
                    .imePadding()
                    .nestedScroll(topBarState.scrollBehavior.nestedScrollConnection)
            ) { paddingValues ->
                when (val mode = listViewDisplayMode()) {
                    is PosterDisplayMode.Grid -> {
                        ContentListPosterGrid(
                            mode = mode,
                            items = state.items,
                            recommendations = state.recommendations,
                            onOptionsClick = onOptionsClick,
                            onLongClick = onLongClick,
                            onClick = onClick,
                            paddingValues = paddingValues,
                            onRefreshClick = refreshRecommendations,
                            refreshingRecommendations = state.refreshingRecommendations,
                            startAddingClick = startAddingClick,
                            isOwnerMe = state.isOwnerMe,
                            modifier = Modifier
                                .haze(
                                    state = hazeState,
                                    style = HazeDefaults.style(MaterialTheme.colorScheme.background),
                                )
                                .padding(top = 12.dp),
                        )
                    }

                    PosterDisplayMode.List -> {
                        ContentListPosterList(
                            items = state.items,
                            onOptionsClick = onOptionsClick,
                            onLongClick = onLongClick,
                            onClick = onClick,
                            paddingValues = paddingValues,
                            onRefreshClick = refreshRecommendations,
                            recommendations = state.recommendations,
                            refreshingRecommendations = state.refreshingRecommendations,
                            onAddRecommendation = onAddRecommendation,
                            onRecommendationClick = onRecommendationClick,
                            onRecommendationLongClick = onRecommendationLongClick,
                            startAddingClick = startAddingClick,
                            isOwnerMe = state.isOwnerMe,
                            modifier = Modifier
                                .haze(
                                    state = hazeState,
                                    style = HazeDefaults.style(MaterialTheme.colorScheme.background),
                                )
                                .padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

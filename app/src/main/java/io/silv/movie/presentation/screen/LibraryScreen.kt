package io.silv.movie.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.PullRefresh
import io.silv.core_ui.components.topbar.rememberPosterTopBarState
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.koin4ScreenModel
import io.silv.movie.presentation.CollectEventsWithLifecycle
import io.silv.movie.presentation.LocalListInteractor
import io.silv.movie.presentation.LocalUser
import io.silv.movie.presentation.components.content.LibraryGridView
import io.silv.movie.presentation.components.content.LibraryListView
import io.silv.movie.presentation.components.dialog.ListOptionsBottomSheet
import io.silv.movie.presentation.components.dialog.RemoveEntryDialog
import io.silv.movie.presentation.components.list.LibraryBrowseTopBar
import io.silv.movie.presentation.covers.EditCoverAction
import io.silv.movie.presentation.covers.LibraryCoverDialog
import io.silv.movie.presentation.covers.screenmodel.ListCoverScreenModel
import io.silv.movie.presentation.screenmodel.LibraryEvent
import io.silv.movie.presentation.screenmodel.LibraryScreenModel
import io.silv.movie.presentation.screenmodel.LibrarySortMode
import io.silv.movie.presentation.screenmodel.LibraryState
import org.koin.core.parameter.parametersOf

data object LibraryScreen: Screen {

    @Composable
    override fun Content() {
        val screenModel = koin4ScreenModel<LibraryScreenModel>()
        val state by screenModel.state.collectAsStateWithLifecycle()
        val listCount by screenModel.listCount.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.currentOrThrow
        val listInteractor = LocalListInteractor.current

        val listCreateScreen = remember(listCount) { ListCreateScreen(listCount) }
        val changeDialog = remember {
            { dialog: LibraryScreenModel.Dialog? -> screenModel.updateDialog(dialog) }
        }

        val createResultLauncher = rememberScreenWithResultLauncher(
            screen = listCreateScreen
        ) { result ->
            screenModel.createList(result.name, result.online)
        }

        CollectEventsWithLifecycle(screenModel) {
            when (it) {
                is LibraryEvent.ListCreated -> {
                    navigator.push(ListViewScreen(it.id))
                }
            }
        }

        LibraryStandardScreenContent(
            query = { screenModel.query },
            sortModeProvider = { screenModel.sortMode },
            listModeProvider = { screenModel.displayInList },
            changeQuery = screenModel::updateQuery,
            setListMode = screenModel::updateListMode,
            changeSortMode = screenModel::updateSortMode,
            createListClicked = { createResultLauncher.launch() },
            onListClick = {
                navigator.push(
                    ListViewScreen(it.id)
                )
            },
            onListLongClick = { list, items ->
                changeDialog(
                    LibraryScreenModel.Dialog.ListOptions(
                        list,
                        items
                    )
                )
            },
            onFavoritesClicked = { navigator.push(FavoritesViewScreen) },
            onPosterClick = { changeDialog(LibraryScreenModel.Dialog.FullCover(it)) },
            refreshLists = screenModel::refreshUserLists,
            refreshFavorites = screenModel::refreshFavoritesList,
            state = state
        )
        val onDismissRequest =  { changeDialog(null) }
        when (val dialog = state.dialog) {
            null -> Unit
            is LibraryScreenModel.Dialog.FullCover -> {
                val sm = koin4ScreenModel<ListCoverScreenModel> { parametersOf(dialog.contentList.id) }

                LaunchedEffect(dialog.contentList.id) {
                    sm.refresh(dialog.contentList.id)
                }

                val list by sm.state.collectAsStateWithLifecycle()
                val context = LocalContext.current

                if (list != null) {
                    val getContent = rememberLauncherForActivityResult(
                        ActivityResultContracts.GetContent(),
                    ) {
                        if (it == null) return@rememberLauncherForActivityResult
                        sm.editCover(context, it)
                    }
                    val items: List<ContentItem> = remember(list) {
                        state.contentLists
                            .firstOrNull { it.first.id == list!!.id }
                            ?.second
                            ?: emptyList()
                    }
                    LibraryCoverDialog(
                        items = items,
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
            is LibraryScreenModel.Dialog.ListOptions -> {
                val listEditScreen =
                    remember(dialog.contentList.name) { ListEditScreen(dialog.contentList.name) }

                val editResultLauncher = rememberScreenWithResultLauncher(
                    screen = listEditScreen
                ) { result ->
                    listInteractor.editList(dialog.contentList) { it.copy(name = result.name) }
                }

                val descriptionEditScreen =
                    remember(dialog.contentList.description) { ListEditDescriptionScreen(dialog.contentList.description) }

                val descriptionResultLauncher = rememberScreenWithResultLauncher(
                    screen = descriptionEditScreen
                ) { result ->
                    listInteractor.editList(dialog.contentList) { it.copy(description = result.description) }
                }

                ListOptionsBottomSheet(
                    onDismissRequest = onDismissRequest,
                    onAddClick = { navigator.push(ListAddScreen(dialog.contentList.id)) },
                    onEditClick = {
                        editResultLauncher.launch()
                        onDismissRequest()
                    },
                    onDeleteClick = { changeDialog(LibraryScreenModel.Dialog.DeleteList(dialog.contentList)) },
                    onShareClick = {
                        listInteractor.toggleListVisibility(dialog.contentList)
                        onDismissRequest()
                    },
                    list = dialog.contentList,
                    onChangeDescription = { descriptionResultLauncher.launch() },
                    onCopyClick = {
                        listInteractor.copyList(dialog.contentList)
                        onDismissRequest()
                    },
                    isUserMe = dialog.contentList.createdBy == LocalUser.current?.userId || dialog.contentList.createdBy == null,
                    content = dialog.items,
                    onSubscribeClicked = {
                        listInteractor.subscribeToList(dialog.contentList)
                        onDismissRequest()
                    },
                    onUnsubscribeClicked = {
                        listInteractor.unsubscribeFromList(dialog.contentList)
                        onDismissRequest()
                    },
                    onTogglePinned = {
                        listInteractor.togglePinned(dialog.contentList)
                        onDismissRequest()
                    }
                )
            }
            is LibraryScreenModel.Dialog.DeleteList -> {
                RemoveEntryDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = { listInteractor.deleteList(dialog.contentList) },
                    entryToRemove = dialog.contentList.name
                )
            }
        }
    }
}

@Composable
private fun LibraryStandardScreenContent(
    query: () -> String,
    sortModeProvider: () -> LibrarySortMode,
    listModeProvider: () -> Boolean,
    setListMode: (Boolean) -> Unit,
    changeQuery: (query: String) -> Unit,
    changeSortMode: (mode: LibrarySortMode) -> Unit,
    createListClicked: () -> Unit,
    onFavoritesClicked: () -> Unit,
    onListLongClick: (contentList: ContentList, items: List<ContentItem>) -> Unit,
    onListClick: (contentList: ContentList) -> Unit,
    onPosterClick: (contentList: ContentList) -> Unit,
    refreshLists: () -> Unit,
    refreshFavorites: () -> Unit,
    state: LibraryState
) {

    val topBarState = rememberPosterTopBarState()
    val scrollBehavior = topBarState.scrollBehavior
    val hazeState = remember { HazeState() }
    val snackBarHostState = remember { SnackbarHostState() }

    PullRefresh(
        refreshing = state.refreshingLists || state.refreshingFavorites,
        enabled = { true },
        onRefresh = {
            refreshLists()
            refreshFavorites()
        }
    ) {
        Scaffold(
            topBar = {
                LibraryBrowseTopBar(
                    modifier = Modifier.hazeChild(hazeState),
                    isListMode = listModeProvider,
                    changeQuery = changeQuery,
                    onSearch = changeQuery,
                    query = query,
                    setListMode = setListMode,
                    changeSortMode = changeSortMode,
                    sortModeProvider =  sortModeProvider,
                    topBarState = topBarState,
                    createListClicked = createListClicked,
                )
            },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets
                .exclude(WindowInsets.systemBars),
            snackbarHost = {
                SnackbarHost(snackBarHostState)
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { paddingValues ->
            if (listModeProvider()) {
                LibraryListView(
                    paddingValues = paddingValues,
                    state = state,
                    onListLongClick = onListLongClick,
                    onListClick = onListClick,
                    onFavoritesClicked = onFavoritesClicked,
                    onPosterClick = onPosterClick,
                    modifier = Modifier.haze(
                        hazeState,
                        HazeDefaults.style(MaterialTheme.colorScheme.background)
                    )
                )
            } else {
                LibraryGridView(
                    paddingValues = paddingValues,
                    state = state,
                    onListLongClick = onListLongClick,
                    onListClick = onListClick,
                    onFavoritesClicked = onFavoritesClicked,
                    onPosterClick = onPosterClick,
                    modifier = Modifier.haze(
                        hazeState,
                        HazeDefaults.style(MaterialTheme.colorScheme.background)
                    )
                )
            }
        }
    }
}

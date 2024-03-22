package io.silv.movie.presentation.library.browse

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.PullRefresh
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListItem
import io.silv.movie.presentation.CollectEventsWithLifecycle
import io.silv.movie.presentation.library.ListCoverScreenModel
import io.silv.movie.presentation.library.ListCreateScreen
import io.silv.movie.presentation.library.components.LibraryCoverDialog
import io.silv.movie.presentation.library.components.LibraryGridView
import io.silv.movie.presentation.library.components.LibraryListView
import io.silv.movie.presentation.library.view.favorite.FavoritesViewScreen
import io.silv.movie.presentation.library.view.list.ListViewScreen
import io.silv.movie.presentation.view.components.EditCoverAction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.koin.core.parameter.parametersOf

class LibraryScreen: Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<LibraryScreenModel>()
        val state by screenModel.state.collectAsStateWithLifecycle()
        val listCount by screenModel.listCount.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.currentOrThrow

        val listCreateScreen = remember(listCount) { ListCreateScreen(listCount) }
        val changeDialog = remember {
            { dialog: LibraryScreenModel.Dialog? -> screenModel.updateDialog(dialog) }
        }

        val screenResultLauncher = rememberScreenWithResultLauncher(
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
            createListClicked = { screenResultLauncher.launch() },
            onListClick = { navigator.push(ListViewScreen(it.id)) },
            onListLongClick = {  },
            onFavoritesClicked = { navigator.push(FavoritesViewScreen) },
            onPosterClick = {
                changeDialog(LibraryScreenModel.Dialog.FullCover(it))
            },
            refreshLists = screenModel::refreshUserLists,
            state = state
        )
        val onDismissRequest =  { changeDialog(null) }
        when (val dialog = state.dialog) {
            null -> Unit
            is LibraryScreenModel.Dialog.FullCover -> {
                val sm = getScreenModel<ListCoverScreenModel> { parametersOf(dialog.contentList.id) }
                val list by sm.state.collectAsStateWithLifecycle()
                val context = LocalContext.current

                if (list != null) {
                    val getContent = rememberLauncherForActivityResult(
                        ActivityResultContracts.GetContent(),
                    ) {
                        if (it == null) return@rememberLauncherForActivityResult
                        sm.editCover(context, it)
                    }
                    val items: ImmutableList<ContentListItem> = remember(list!!.posterLastModified) {
                         state.contentLists
                            .firstOrNull { it.first.id == list!!.id }
                            ?.second
                            ?: persistentListOf()
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
    onListLongClick: (contentList: ContentList) -> Unit,
    onListClick: (contentList: ContentList) -> Unit,
    onPosterClick: (contentList: ContentList) -> Unit,
    refreshLists: () -> Unit,
    state: LibraryState
) {

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val snackBarHostState = remember { SnackbarHostState() }

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
                scrollBehavior = scrollBehavior,
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
        PullRefresh(
            refreshing = state.refreshingLists,
            enabled = { !state.refreshingLists },
            onRefresh = { refreshLists() },
            indicatorPadding = paddingValues
        ) {
        Crossfade(
            targetState = listModeProvider(),
            label = "display-mode-crossfade"
        ) { isList ->
            if (isList) {
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
}

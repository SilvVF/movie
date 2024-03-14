package io.silv.movie.presentation.library.view.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.rememberPosterTopBarState
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.CollectEventsWithLifecycle
import io.silv.movie.presentation.browse.components.RemoveEntryDialog
import io.silv.movie.presentation.library.ListEditScreen
import io.silv.movie.presentation.library.components.ContentListPosterGrid
import io.silv.movie.presentation.library.components.ContentListPosterList
import org.koin.core.parameter.parametersOf

data class ListViewScreen(
    private val listId: Long,
): Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<ListViewScreenModel> { parametersOf(listId) }
        val state by screenModel.state.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.currentOrThrow

        CollectEventsWithLifecycle(screenModel) { event ->
            when(event) {
                ListViewEvent.ListDeleted -> navigator.pop()
            }
        }

        when (val s = state) {
            is ListViewState.Error -> Unit
            ListViewState.Loading -> Unit
            is ListViewState.Success -> {

                val listEditScreen = remember(s.list.name) { ListEditScreen(s.list.name) }

                val screenResultLauncher = rememberScreenWithResultLauncher(
                    screen = listEditScreen
                ) { result ->
                    screenModel.editList(s.list, result.name)
                }

                val changeDialog = remember {
                    { sheet: ListViewScreenModel.Dialog? -> screenModel.changeDialog(sheet) }
                }

                SuccessScreenContent(
                    query = screenModel.query,
                    updateQuery = screenModel::updateQuery,
                    onListOptionClick = { changeDialog(ListViewScreenModel.Dialog.ListOptions) },
                    updateListViewDisplayMode = screenModel::updateListViewDisplayMode,
                    listViewDisplayMode = { screenModel.listViewDisplayMode },
                    onClick = { },
                    onLongClick = { },
                    onOptionsClick = { changeDialog(ListViewScreenModel.Dialog.ContentOptions(it)) },
                    updateDialog = { changeDialog(it) },
                    changeSortMode = screenModel::updateSortMode,
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
                        ModalBottomSheet(onDismissRequest = onDismissRequest) {
                            Box(
                                Modifier
                                    .fillMaxHeight(0.5f)
                                    .fillMaxWidth()) {
                                Text("Content Options")
                            }
                        }
                    }
                    ListViewScreenModel.Dialog.ListOptions -> {
                        ListOptionsBottomSheet(
                            onDismissRequest = onDismissRequest,
                            onAddClick = {},
                            onEditClick = { screenResultLauncher.launch() },
                            onDeleteClick = { changeDialog(ListViewScreenModel.Dialog.DeleteList) },
                            onShareClick = {},
                            list = s.list,
                            content = s.allItems
                        )
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
    state: ListViewState.Success
) {
    val topBarState = rememberPosterTopBarState()
    val hazeState = remember { HazeState() }

    Scaffold(
        topBar = {
            ListViewTopBar(
                state  = topBarState,
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
                    onOptionsClick = onOptionsClick,
                    onLongClick = onLongClick,
                    onClick = onClick,
                    paddingValues = paddingValues,
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



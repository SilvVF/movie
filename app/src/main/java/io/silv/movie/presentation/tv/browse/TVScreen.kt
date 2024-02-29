package io.silv.movie.presentation.tv.browse

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.movie.data.movie.model.ContentPagedType
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.data.tv.TVShow
import io.silv.movie.presentation.LocalIsScrolling
import io.silv.movie.presentation.movie.browse.MovieScreen
import io.silv.movie.presentation.movie.browse.RemoveEntryDialog
import io.silv.movie.presentation.movie.browse.components.ContentBrowseTopBar
import io.silv.movie.presentation.movie.browse.components.FilterBottomSheet
import io.silv.movie.presentation.tv.components.TVSourcePagingContent
import io.silv.movie.presentation.tv.view.TVViewScreen
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.parameter.parametersOf

data class TVScreen(
    val query: String? = null
): Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<TVScreenModel> { parametersOf(query.orEmpty()) }

        val state by screenModel.state.collectAsStateWithLifecycle()

        // needed for movie action to be stable
        val stableChangeDialogRefrence = { dialog: TVScreenModel.Dialog.RemoveShow ->
            screenModel.changeDialog(dialog)
        }
        val toggleMovieFavorite = { show: TVShow -> screenModel.toggleShowFavorite(show) }
        val navigator = LocalNavigator.currentOrThrow

        TVStandardScreenSizeContent(
            pagingFlowFlow = { screenModel.tvPagerFlowFlow },
            displayMode = { screenModel.displayMode },
            gridCellsCount = { screenModel.gridCells },
            query = { state.query },
            listing = { state.listing },
            changeDialog = screenModel::changeDialog,
            actions = TVActions(
                changeCategory = screenModel::changeCategory,
                changeQuery = screenModel::changeQuery,
                showLongClick = {
                    if(it.favorite) {
                        stableChangeDialogRefrence(TVScreenModel.Dialog.RemoveShow(it))
                    } else {
                        toggleMovieFavorite(it)
                    }
                },
                showClick = { navigator.push(TVViewScreen(it.id)) },
                onSearch = screenModel::onSearch,
                setDisplayMode = screenModel::changeDisplayMode,
                changeGridCellCount = screenModel::changeGridCells
            ),
            changeResourceType = {
                navigator.replace(MovieScreen(state.query))
            }
        )
        val onDismissRequest = { screenModel.changeDialog(null) }
        when (val dialog = state.dialog) {
            is TVScreenModel.Dialog.RemoveShow -> {
                RemoveEntryDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = { screenModel.toggleShowFavorite(dialog.show) },
                    entryToRemove = dialog.show.title
                )
            }
            TVScreenModel.Dialog.Filter -> {
                FilterBottomSheet(
                    onDismissRequest = onDismissRequest,
                    onApplyFilter = { /*TODO*/ }
                ) {

                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun TVStandardScreenSizeContent(
    listing: () -> ContentPagedType,
    query: () -> String,
    displayMode: () -> PosterDisplayMode,
    pagingFlowFlow: () -> StateFlow<PagingData<StateFlow<TVShow>>>,
    gridCellsCount: () -> Int,
    changeResourceType: () -> Unit,
    changeDialog: (TVScreenModel.Dialog?) -> Unit,
    actions: TVActions
) {

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val snackBarHostState = remember { SnackbarHostState() }

    val isScrolling = remember { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalIsScrolling provides isScrolling
    ) {
        Scaffold(
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets
                .exclude(WindowInsets.systemBars),
            snackbarHost = {
                SnackbarHost(snackBarHostState)
            },
            topBar = {
                ContentBrowseTopBar(
                    modifier = Modifier.hazeChild(hazeState),
                    query = query,
                    isMovie = false,
                    listing = listing,
                    displayMode = displayMode,
                    scrollBehavior = scrollBehavior,
                    changePagedType = { actions.changeCategory(it) },
                    changeResourceType = changeResourceType,
                    setDisplayMode = { actions.setDisplayMode(it) },
                    onSearch = { actions.onSearch(it) },
                    changeQuery = { actions.changeQuery(it) }
                )
            },
            floatingActionButton = {
                val expanded by LocalIsScrolling.current
                ExtendedFloatingActionButton(
                    onClick = { changeDialog(TVScreenModel.Dialog.Filter) },
                    text = {
                        Text(text = "Filter")
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = null
                        )
                    },
                    expanded = expanded,
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { paddingValues ->
            TVSourcePagingContent(
                paddingValues = paddingValues,
                pagingFlowFlow = pagingFlowFlow,
                snackbarHostState = snackBarHostState,
                modifier = Modifier
                    .fillMaxSize()
                    .haze(
                        state = hazeState,
                        style = HazeDefaults.style(backgroundColor = MaterialTheme.colorScheme.background),
                    ),
                actions = actions,
                displayMode = displayMode,
                gridCellsCount = gridCellsCount
            )
        }
    }
}
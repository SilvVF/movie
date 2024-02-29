package io.silv.movie.presentation.movie.browse

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import io.silv.movie.data.movie.model.Movie
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.LocalIsScrolling
import io.silv.movie.presentation.movie.browse.components.ContentBrowseTopBar
import io.silv.movie.presentation.movie.browse.components.FilterBottomSheet
import io.silv.movie.presentation.movie.browse.components.MovieSourcePagingContent
import io.silv.movie.presentation.movie.view.MovieViewScreen
import io.silv.movie.presentation.tv.browse.TVScreen
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.parameter.parametersOf

data class MovieScreen(
    val query: String? = null
): Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<MovieScreenModel> { parametersOf(query.orEmpty()) }

        val state by screenModel.state.collectAsStateWithLifecycle()

        // needed for movie action to be stable
        val stableChangeDialogRefrence = { dialog: MovieScreenModel.Dialog.RemoveMovie ->
            screenModel.changeDialog(dialog)
        }
        val toggleMovieFavorite = { movie: Movie -> screenModel.toggleMovieFavorite(movie) }
        val navigator = LocalNavigator.currentOrThrow

        MovieStandardScreenSizeContent(
            pagingFlowFlow = { screenModel.moviePagerFlowFlow },
            displayMode = { screenModel.displayMode },
            gridCellsCount = { screenModel.gridCells },
            query = { state.query },
            listing = { state.listing },
            changeDialog = screenModel::changeDialog,
            actions = MovieActions(
                changeCategory = screenModel::changeCategory,
                changeQuery = screenModel::changeQuery,
                movieLongClick = {
                    if(it.favorite) {
                        stableChangeDialogRefrence(MovieScreenModel.Dialog.RemoveMovie(it))
                    } else {
                        toggleMovieFavorite(it)
                    }
                },
                movieClick = { navigator.push(MovieViewScreen(it.id)) },
                onSearch = screenModel::onSearch,
                setDisplayMode = screenModel::changeDisplayMode,
                changeGridCellCount = screenModel::changeGridCells
            ),
            changeResourceType = {
                navigator.replace(TVScreen(state.query))
            }
        )
        val onDismissRequest = { screenModel.changeDialog(null) }
        when (val dialog = state.dialog) {
            is MovieScreenModel.Dialog.RemoveMovie -> {
                RemoveEntryDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = { screenModel.toggleMovieFavorite(dialog.movie) },
                    entryToRemove = dialog.movie.title
                )
            }
            MovieScreenModel.Dialog.Filter -> {
                FilterBottomSheet(
                    onDismissRequest = onDismissRequest,
                    onApplyFilter = { /*TODO*/ }) {

                }
            }
            else -> Unit
        }
    }
}


@Composable
private fun MovieStandardScreenSizeContent(
    listing: () -> ContentPagedType,
    query: () -> String,
    displayMode: () -> PosterDisplayMode,
    pagingFlowFlow: () -> StateFlow<PagingData<StateFlow<Movie>>>,
    gridCellsCount: () -> Int,
    changeResourceType: () -> Unit,
    changeDialog: (MovieScreenModel.Dialog?) -> Unit,
    actions: MovieActions
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
                    isMovie = true,
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
                    onClick = { changeDialog(MovieScreenModel.Dialog.Filter) },
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
            MovieSourcePagingContent(
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



@Composable
fun RemoveEntryDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    entryToRemove: String,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "cancel")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirm()
                },
            ) {
                Text(text = "remove")
            }
        },
        title = {
            Text(text = "Are you sure?")
        },
        text = {
            Text(text = "You are about to remove \"${entryToRemove}\" from your library")
        },
    )
}


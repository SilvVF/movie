package io.silv.movie.presentation.movie

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.data.Movie
import io.silv.data.MoviePagedType
import io.silv.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.movie.components.MovieSourcePagingContent
import io.silv.movie.presentation.movie.components.MovieTopAppBar
import kotlinx.coroutines.flow.Flow
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
        val stableChangeDialogRefrence = screenModel::changeDialog

        MovieStandardScreenSizeContent(
            pagingFlowFlow = { screenModel.moviePagerFlowFlow },
            displayMode = { screenModel.displayMode },
            gridCellsCount = { screenModel.gridCells },
            resource = { state.resource },
            query = { state.query },
            listing = { state.listing },
            actions = MovieActions(
                changeCategory = screenModel::changeCategory,
                changeQuery = screenModel::changeQuery,
                changeResource = screenModel::changeResource,
                movieLongClick = {
                    if(it.favorite) {
                        stableChangeDialogRefrence.invoke(MovieScreenModel.Dialog.RemoveMovie(it))
                    }
                },
                movieClick = {},
                onSearch = screenModel::onSearch,
                setDisplayMode = screenModel::changeDisplayMode,
                changeGridCellCount = screenModel::changeGridCells
            )
        )
        val onDismissRequest = { screenModel.changeDialog(null) }
        when (val dialog = state.dialog) {
            is MovieScreenModel.Dialog.RemoveMovie -> {
                RemoveEntryDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = { },
                    entryToRemove = dialog.movie.title
                )
            }
            else -> Unit
        }
    }
}

@Composable
private fun MovieStandardScreenSizeContent(
    listing: () -> MoviePagedType,
    query: () -> String,
    displayMode: () -> PosterDisplayMode,
    pagingFlowFlow: () -> StateFlow<Flow<PagingData<StateFlow<Movie>>>>,
    gridCellsCount: () -> Int,
    resource: () -> Resource,
    actions: MovieActions
) {

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val snackBarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackBarHostState)
        },
        topBar = {
            Column(
                Modifier.hazeChild(hazeState)
            ) {
                MovieTopAppBar(
                    query =  query,
                    resource = resource,
                    listing = listing,
                    displayMode = displayMode,
                    scrollBehavior = scrollBehavior,
                    actions = actions,
                )
            }
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


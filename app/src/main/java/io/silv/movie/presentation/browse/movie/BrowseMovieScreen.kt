package io.silv.movie.presentation.browse.movie

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
import cafe.adriel.voyager.core.lifecycle.DisposableEffectIgnoringConfiguration
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.movie.data.ContentPagedType
import io.silv.movie.data.movie.model.MoviePoster
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.browse.LocalIsScrolling
import io.silv.movie.presentation.browse.components.FilterBottomSheet
import io.silv.movie.presentation.browse.components.RemoveEntryDialog
import io.silv.movie.presentation.browse.movie.components.ContentBrowseTopBar
import io.silv.movie.presentation.browse.movie.components.MovieSourcePagingContent
import io.silv.movie.presentation.browse.tv.BrowseTVScreen
import io.silv.movie.presentation.view.movie.MovieViewScreen
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.parameter.parametersOf

data class BrowseMovieScreen(
    var contentPagedType: ContentPagedType
): Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<MovieScreenModel> { parametersOf(contentPagedType) }

        val state by screenModel.state.collectAsStateWithLifecycle()

        // needed for movie action to be stable
        val stableChangeDialogRefrence = remember {
            { dialog: MovieScreenModel.Dialog.RemoveMovie ->
                screenModel.changeDialog(dialog)
            }
        }
        val toggleMovieFavorite = remember {
            { movie: MoviePoster -> screenModel.toggleMovieFavorite(movie) }
        }
        val navigator = LocalNavigator.currentOrThrow

        DisposableEffectIgnoringConfiguration(Unit) {
            onDispose { contentPagedType = state.listing }
        }

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
                navigator.replace(
                    BrowseTVScreen(state.listing)
                )
            }
        )
        val onDismissRequest = remember {{ screenModel.changeDialog(null) }}
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
                    onApplyFilter = {
                        onDismissRequest()
                        screenModel.changeCategory(ContentPagedType.Discover(state.filters))
                    },
                    onResetFilter = screenModel::resetFilters,
                    genres = state.genreItems,
                    searchItems = screenModel.searchItems,
                    onGenreSelected = {
                        screenModel.changeFilters { filters ->
                            filters.copy(
                            genres = if (state.filters.genres.contains(it))
                               filters.genres - it
                            else
                                filters.genres + it
                        )
                    }
                    },
                    onSortingItemSelected = {
                        screenModel.changeFilters { filters ->
                           filters.copy(sortingOption = it)
                        }
                    },
                    selectedSortingOption = state.filters.sortingOption,
                    genreMode = state.filters.genreMode,
                    changeGenreMode = {
                        screenModel.changeFilters { filters ->
                            filters.copy(genreMode = it)
                        }
                    }
                )
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
    pagingFlowFlow: () -> StateFlow<PagingData<StateFlow<MoviePoster>>>,
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
                    changeQuery = { actions.changeQuery(it) },
                    onFilterClick = {changeDialog(MovieScreenModel.Dialog.Filter) }
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
                        style = HazeDefaults
                            .style(
                                backgroundColor = MaterialTheme.colorScheme.background
                            ),
                    ),
                actions = actions,
                displayMode = displayMode,
                gridCellsCount = gridCellsCount
            )
        }
    }
}





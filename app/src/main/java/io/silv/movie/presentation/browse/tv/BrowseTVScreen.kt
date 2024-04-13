package io.silv.movie.presentation.browse.tv

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExploreOff
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import io.silv.core_ui.components.Action
import io.silv.core_ui.components.EmptyScreen
import io.silv.movie.R
import io.silv.movie.data.ContentPagedType
import io.silv.movie.data.lists.toContentItem
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.data.tv.model.TVShowPoster
import io.silv.movie.presentation.LocalContentInteractor
import io.silv.movie.presentation.browse.LocalIsScrolling
import io.silv.movie.presentation.browse.components.FilterBottomSheet
import io.silv.movie.presentation.browse.components.RemoveEntryDialog
import io.silv.movie.presentation.browse.movie.BrowseMovieScreen
import io.silv.movie.presentation.browse.movie.components.ContentBrowseTopBar
import io.silv.movie.presentation.browse.tv.components.TVSourcePagingContent
import io.silv.movie.presentation.view.tv.TVViewScreen
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.parameter.parametersOf

data class BrowseTVScreen(
    var contentPagedType: ContentPagedType
): Screen {


    @Composable
    override fun Content() {

        val screenModel = getScreenModel<TVScreenModel> { parametersOf(contentPagedType) }

        val state by screenModel.state.collectAsStateWithLifecycle()
        val online by screenModel.online.collectAsStateWithLifecycle()
        // needed for movie action to be stable
        val stableChangeDialogRefrence = remember {
            { dialog: TVScreenModel.Dialog.RemoveShow ->
                screenModel.changeDialog(dialog)
            }
        }
        val contentInteractor = LocalContentInteractor.current
        val navigator = LocalNavigator.currentOrThrow

        DisposableEffectIgnoringConfiguration(Unit) {
            onDispose { contentPagedType = state.listing }
        }

        TVStandardScreenSizeContent(
            pagingFlowFlow = { screenModel.tvPagerFlowFlow },
            displayMode = { screenModel.displayMode },
            gridCellsCount = { screenModel.gridCells },
            query = { screenModel.query },
            listing = { state.listing },
            changeDialog = screenModel::changeDialog,
            actions = TVActions(
                changeCategory = screenModel::changeCategory,
                changeQuery = screenModel::changeQuery,
                showLongClick = {
                    if(it.favorite) {
                        stableChangeDialogRefrence(TVScreenModel.Dialog.RemoveShow(it))
                    } else {
                        contentInteractor.toggleFavorite(it.toContentItem())
                    }
                },
                showClick = { navigator.push(TVViewScreen(it.id)) },
                onSearch = screenModel::onSearch,
                setDisplayMode = screenModel::changeDisplayMode,
                changeGridCellCount = screenModel::changeGridCells
            ),
            online = online,
            changeResourceType = {
                navigator.replace(
                    BrowseMovieScreen(state.listing)
                )
            }
        )
        val onDismissRequest = { screenModel.changeDialog(null) }
        when (val dialog = state.dialog) {
            is TVScreenModel.Dialog.RemoveShow -> {
                RemoveEntryDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = { contentInteractor.toggleFavorite(dialog.show.toContentItem()) },
                    entryToRemove = dialog.show.title
                )
            }
            TVScreenModel.Dialog.Filter -> {
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
private fun TVStandardScreenSizeContent(
    listing: () -> ContentPagedType,
    query: () -> String,
    displayMode: () -> PosterDisplayMode,
    pagingFlowFlow: () -> StateFlow<PagingData<StateFlow<TVShowPoster>>>,
    gridCellsCount: () -> Int,
    changeResourceType: () -> Unit,
    online: Boolean,
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
                    changeQuery = { actions.changeQuery(it) },
                    onFilterClick = {changeDialog(TVScreenModel.Dialog.Filter) }
                )
            },
            floatingActionButton = {
                val expanded by LocalIsScrolling.current
                ExtendedFloatingActionButton(
                    onClick = { changeDialog(TVScreenModel.Dialog.Filter) },
                    text = {
                        Text(text = stringResource(id = R.string.filter))
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = stringResource(id = R.string.filter)
                        )
                    },
                    expanded = expanded,
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { paddingValues ->
            if (online) {
                TVSourcePagingContent(
                    paddingValues = paddingValues,
                    pagingFlowFlow = pagingFlowFlow,
                    snackbarHostState = snackBarHostState,
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(
                            state = hazeState,
                            style = HazeDefaults.style(MaterialTheme.colorScheme.background),
                        ),
                    actions = actions,
                    displayMode = displayMode,
                    gridCellsCount = gridCellsCount
                )
            } else {
                val context = LocalContext.current
                EmptyScreen(
                    icon = Icons.Filled.ExploreOff,
                    iconSize = 182.dp,
                    contentPadding = paddingValues,
                    message = stringResource(id = R.string.no_internet_error),
                    actions = persistentListOf(
                        Action(
                            R.string.take_to_settings,
                            onClick = {
                                try {
                                    val settingsIntent: Intent =
                                        Intent(Settings.ACTION_WIRELESS_SETTINGS)
                                    context.startActivity(settingsIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    )
                )
            }
        }
    }
}
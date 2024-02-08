package io.silv.movie.presentation.movie

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.Badge
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryComfortableGridItem
import io.silv.core_ui.components.EntryCompactGridItem
import io.silv.core_ui.components.SearchBarInputField
import io.silv.core_ui.components.SearchLargeTopBar
import io.silv.core_ui.components.TooltipIconButton
import io.silv.core_ui.components.colors2
import io.silv.core_ui.components.loadingIndicatorItem
import io.silv.core_ui.components.toPoster
import io.silv.data.Movie
import io.silv.data.MoviePagedType
import io.silv.data.prefrences.PosterDisplayMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest

object MovieScreen: Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<MovieScreenModel>()

        val state by screenModel.state.collectAsStateWithLifecycle()

        MovieScreenContent(
            state = state,
            pagingFlowFlow = { screenModel.moviePagerFlowFlow },
            displayMode = { screenModel.displayMode },
            actions = MovieActions(
                changeCategory = screenModel::changeCategory,
                changeQuery = screenModel::changeQuery,
                changeResource = screenModel::changeResource,
                movieLongClick = {
                    if (it.favorite) {
                        Log.d("Called", it.toString())
                        screenModel.changeDialog(MovieScreenModel.Dialog.RemoveMovie(it))
                    }
                },
                movieClick = {},
                onSearch = screenModel::onSearch,
                setDisplayMode = screenModel::changeDisplayMode
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
private fun MovieScreenContent(
    state: MovieState,
    displayMode: () -> PosterDisplayMode,
    pagingFlowFlow: () -> StateFlow<Flow<PagingData<StateFlow<Movie>>>>,
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
                    state = state,
                    scrollBehavior = scrollBehavior,
                    actions = actions,
                    displayMode = displayMode
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        MoviePagedGrid(
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
            displayMode = displayMode
        )
    }
}

@Composable
fun MovieTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    state: MovieState,
    actions: MovieActions,
    displayMode: () -> PosterDisplayMode
) {
    val barExpandedFully by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction == 0.0f }
    }

    val barFullyCollapsed by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction == 1f }
    }

    val colors = TopAppBarDefaults.colors2(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = if (barFullyCollapsed)
            Color.Transparent
        else {
            MaterialTheme.colorScheme.surface
        }
    )

    val appBarContainerColor by rememberUpdatedState(
        colors.containerColor(scrollBehavior.state.collapsedFraction)
    )

    SearchLargeTopBar(
        title = { Text("TMDB") },
        scrollBehavior = scrollBehavior,
        colors = colors,
        modifier = modifier.fillMaxWidth(),
        actions = {
            ResourceFilterChips(
                changeResourceType = { actions.changeResource(it) },
                selected = state.resource
            )
            var dropDownVisible by remember {
                mutableStateOf(false)
            }
            Box(contentAlignment = Alignment.BottomCenter) {
                DropdownMenu(
                    expanded = dropDownVisible,
                    onDismissRequest = { dropDownVisible = false }
                ) {
                    PosterDisplayMode.values.forEach {
                        DropdownMenuItem(
                            trailingIcon = {
                                RadioButton(
                                    selected = displayMode() == it,
                                    onClick = { actions.setDisplayMode(it) }
                                )
                            },
                            text = {
                                Text(
                                    remember {
                                        it.toString()
                                            .split(Regex("(?<=[a-z])(?=[A-Z])"))
                                            .joinToString(" ")
                                    }
                                )
                            },
                            onClick = { actions.setDisplayMode(it) }
                        )
                    }
                }
                TooltipIconButton(
                    onClick = { dropDownVisible = true },
                    imageVector = when(displayMode()) {
                        PosterDisplayMode.List -> Icons.AutoMirrored.Filled.List
                        else -> Icons.Filled.GridView
                    },
                    contentDescription = null,
                    tooltip = "Display Mode"
                )
            }
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = null
                )
            }
        },
    ) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        SearchBarInputField(
            query = state.query,
            placeholder = { Text("Search for ${state.resource}...") },
            onQueryChange = { actions.changeQuery(it) },
            focusRequester = focusRequester,
            onSearch = {
                actions.onSearch(it)
                keyboardController?.hide()
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(visible = state.query.isNotEmpty()) {
                        IconButton(
                            onClick = { actions.changeQuery("") }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = null
                            )
                        }
                    }
                    IconButton(
                        onClick = { actions.onSearch(state.query) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null
                        )
                    }
                }
            },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
    Surface(
        color = if(barExpandedFully)
            colors.containerColor
        else
            appBarContainerColor
    ) {
        MovieFilterChips(
            selected = state.listing,
            query = state.query,
            changeMovePagesType = {
                actions.changeCategory(it)
            }
        )
    }
}

@Composable
fun RowScope.ResourceFilterChips(
    changeResourceType: (Resource) -> Unit,
    selected: Resource,
) {

    TooltipIconButton(
        onClick = { changeResourceType(Resource.Movie) },
        imageVector = Icons.Filled.Movie,
        tooltip = "Movies",
        tint = if (selected == Resource.Movie) {
            MaterialTheme.colorScheme.primary
        } else {
            LocalContentColor.current
        }
    )
    TooltipIconButton(
        onClick = { changeResourceType(Resource.TVShow) },
        imageVector = Icons.Filled.Tv,
        tooltip = "TV Shows",
        contentDescription = null,
        tint = if (selected == Resource.TVShow) {
            MaterialTheme.colorScheme.primary
        } else {
            LocalContentColor.current
        }
    )
}

@Composable
fun MovieFilterChips(
    changeMovePagesType: (MoviePagedType) -> Unit,
    selected: MoviePagedType,
    query: String,
) {
    val filters =
        remember {
            listOf(
                Triple("Popular", Icons.Filled.Whatshot, MoviePagedType.Default.Popular),
                Triple("Top Rated", Icons.Outlined.AutoAwesome, MoviePagedType.Default.TopRated),
                Triple("Upcoming", Icons.Filled.NewReleases, MoviePagedType.Default.Upcoming),
                Triple("Filter", Icons.Filled.FilterList, MoviePagedType.Search(query)),
            )
        }

    LazyRow {
        filters.fastForEach { (tag, icon, type) ->
            item(
                key = tag,
            ) {
                FilterChip(
                    modifier = Modifier.padding(4.dp),
                    selected = selected::class == type::class,
                    onClick = {
                        changeMovePagesType(type)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = icon.name,
                        )
                    },
                    label = {
                        Text(text = tag)
                    },
                )
            }
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

@Composable
fun InLibraryBadge(enabled: Boolean) {
    if (enabled) {
        Badge(imageVector = Icons.Outlined.CollectionsBookmark,)
    }
}

@Composable
private fun BrowseMovieSourceCompactGridItem(
    movieFlow: StateFlow<Movie>,
    onClick: (Movie) -> Unit = {},
    onLongClick: (Movie) -> Unit = {},
) {
    val movie by movieFlow.collectAsStateWithLifecycle()
    EntryCompactGridItem(
        title = movie.title,
        coverData = movie.toPoster(),
        coverAlpha = if (movie.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = movie.favorite) },
        onLongClick = { onLongClick(movie) },
        onClick = { onClick(movie) },
    )
}

@Composable
private fun BrowseMovieSourceComfortableGridItem(
    movieFlow: StateFlow<Movie>,
    onClick: (Movie) -> Unit = {},
    onLongClick: (Movie) -> Unit = {},
) {
    val movie by movieFlow.collectAsStateWithLifecycle()

    EntryComfortableGridItem(
        title = movie.title,
        coverData = movie.toPoster(),
        coverAlpha = if (movie.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = { InLibraryBadge(enabled = movie.favorite) },
        onLongClick = { onLongClick(movie) },
        onClick = { onClick(movie) },
    )
}

@Composable
fun MoviePagedGrid(
    pagingFlowFlow: () -> StateFlow<Flow<PagingData<StateFlow<Movie>>>>,
    snackbarHostState: SnackbarHostState,
    displayMode: () -> PosterDisplayMode,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    actions: MovieActions,
) {
    val pagingFlow by pagingFlowFlow().collectAsStateWithLifecycle()
    val pagingItems = pagingFlow.collectAsLazyPagingItems()

    LaunchedEffect(pagingItems) {
        snapshotFlow { pagingItems.loadState.append }.collectLatest { loadState ->
            when(loadState) {
                is LoadState.Error -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Failed to load items",
                        withDismissAction = false,
                        actionLabel = "Retry",
                        duration = SnackbarDuration.Indefinite
                    )
                    when (result) {
                        SnackbarResult.ActionPerformed -> pagingItems.retry()
                        else -> Unit
                    }
                }
                else -> Unit
            }
        }
    }
    if (pagingItems.loadState.refresh is LoadState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (pagingItems.loadState.refresh is LoadState.Error) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("error")
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = paddingValues,
        modifier = modifier.padding(top = 12.dp)
    ) {
        when (displayMode()) {
            PosterDisplayMode.ComfortableGrid -> {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.value.id },
                    contentType = pagingItems.itemContentType()
                ) {
                    val movieFlow = pagingItems[it]

                    movieFlow?.let { flow ->
                        BrowseMovieSourceComfortableGridItem(
                            flow,
                            onClick = actions.movieLongClick,
                            onLongClick = actions.movieLongClick
                        )
                    }
                }
            }
            PosterDisplayMode.CompactGrid -> {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.value.id },
                    contentType = pagingItems.itemContentType()
                ) {
                    val movieFlow = pagingItems[it]

                    movieFlow?.let { flow ->
                        BrowseMovieSourceCompactGridItem(
                            flow,
                            onClick = actions.movieClick,
                            onLongClick = actions.movieLongClick
                        )
                    }
                }
            }
            PosterDisplayMode.CoverOnlyGrid -> {}
            PosterDisplayMode.List -> {}
        }
        loadingIndicatorItem(pagingItems)
    }
}
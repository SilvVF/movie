package io.silv.movie.presentation.movie.discover

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryCompactGridItem
import io.silv.core_ui.components.toPoster
import io.silv.data.movie.model.Genre
import io.silv.data.movie.model.Movie
import io.silv.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.movie.browse.Resource
import io.silv.movie.presentation.movie.browse.components.InLibraryBadge
import io.silv.movie.presentation.movie.discover.components.CategorySelectDialog
import io.silv.movie.presentation.movie.discover.components.MovieDiscoverTopBar
import io.silv.movie.presentation.movie.discover.components.SelectedPagingItemsGrid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import org.koin.core.parameter.parametersOf

object MovieDiscoverTab: Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = "Discover",
            icon = rememberVectorPainter(image = Icons.Filled.FilterNone)
        )

    private var genreSavedState: Genre? = null
    private var resourceSavedState: Resource? = null

    @Composable
    override fun Content() {


        val screenModel = getScreenModel<MovieDiscoverScreenModel>() { parametersOf(genreSavedState, resourceSavedState) }
        val state by screenModel.state.collectAsStateWithLifecycle()
        val selectedPagingData by screenModel.pagingFlow.collectAsStateWithLifecycle()

        val pagingItems = selectedPagingData?.collectAsLazyPagingItems()

        LaunchedEffect(screenModel) {
            snapshotFlow { state }.collectLatest {
                genreSavedState = it.selectedGenre
                resourceSavedState = it.selectedResource
            }
        }

        MovieDiscoverTabContent(
            setCurrentDialog = screenModel::changeDialog,
            moviesByGenre = state.genreWithMovie,
            selectedPagingItems = pagingItems,
            selectedGenre = { state.selectedGenre },
            selectedResource = { state.selectedResource },
            onResourceSelected = screenModel::onResourceSelected,
            clearFilters = screenModel::clearAllSelection
        )

        val onDismissRequest = { screenModel.changeDialog(null) }
        when (state.dialog) {
            MovieDiscoverScreenModel.Dialog.CategorySelect -> {
                CategorySelectDialog(
                    onDismissRequest = onDismissRequest,
                    selectedGenre = state.selectedGenre,
                    genres = state.genres,
                    onGenreSelected = screenModel::onGenreSelected,
                    clearAllSelected = {},
                )
            }
            null -> Unit
        }
    }
}


@Composable
fun MovieDiscoverTabContent(
    selectedGenre: () -> Genre?,
    selectedResource: () -> Resource?,
    selectedPagingItems: LazyPagingItems<StateFlow<Movie>>?,
    setCurrentDialog: (MovieDiscoverScreenModel.Dialog?) -> Unit,
    moviesByGenre: ImmutableList<Pair<Genre, ImmutableList<StateFlow<Movie>>>>,
    onResourceSelected: (Resource?) -> Unit,
    clearFilters: () -> Unit,
) {
    val hazeState = remember { HazeState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
           MovieDiscoverTopBar(
               modifier = Modifier
                   .fillMaxWidth()
                   .hazeChild(
                       state = hazeState,
                       style = HazeDefaults.style(
                           backgroundColor = MaterialTheme.colorScheme.background
                       )
                   ),
               setCurrentDialog = setCurrentDialog,
               scrollBehavior = scrollBehavior,
               selectedGenre = selectedGenre,
               selectedResource = selectedResource,
               onResourceSelected = onResourceSelected,
               clearFilters = clearFilters
           )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        AnimatedContent(
            selectedPagingItems,
            label =""
        ) { items ->
            if (items != null) {
                SelectedPagingItemsGrid(
                    mode = PosterDisplayMode.Grid.CoverOnlyGrid,
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(hazeState),
                    gridCellsCount = { 2 },
                    paddingValues = paddingValues,
                    pagingItems = items,
                    onMovieClick = {},
                    onMovieLongClick = {}
                )
            } else {
                GenreItemsLists(
                    paddingValues = paddingValues,
                    hazeState = hazeState,
                    moviesByGenre = moviesByGenre,
                    onMovieLongClick = {},
                    onMovieClick = {}
                )
            }
        }
    }
}

@Composable
fun GenreItemsLists(
    paddingValues: PaddingValues,
    hazeState: HazeState,
    moviesByGenre: ImmutableList<Pair<Genre, ImmutableList<StateFlow<Movie>>>>,
    onMovieLongClick: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
) {
    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier
            .fillMaxSize()
            .haze(hazeState)
    ) {
        items(
            items = moviesByGenre,
            key = { it.first.name + it.first.name }
        ) { (genre, items) ->

            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = genre.name,
                    style = MaterialTheme.typography.titleLarge
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    items(
                        items = items,
                        key = { it.value.id },
                    ) { movieStateFlow ->

                        val movie by movieStateFlow.collectAsStateWithLifecycle()

                        Box(
                            Modifier
                                .width(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            EntryCompactGridItem(
                                coverData = movie.toPoster(),
                                coverAlpha = if (movie.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
                                coverBadgeStart = { InLibraryBadge(enabled = movie.favorite) },
                                onLongClick = { onMovieLongClick(movie) },
                                onClick = { onMovieClick(movie) },
                            )
                        }
                    }
                }
            }
        }
    }
}
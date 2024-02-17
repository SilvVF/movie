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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.CommonEntryItemDefaults
import io.silv.core_ui.components.EntryCompactGridItem
import io.silv.core_ui.components.toPoster
import io.silv.data.movie.model.Genre
import io.silv.data.movie.model.Movie
import io.silv.data.movie.model.TVShow
import io.silv.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.movie.browse.RemoveEntryDialog
import io.silv.movie.presentation.movie.browse.Resource
import io.silv.movie.presentation.movie.browse.components.InLibraryBadge
import io.silv.movie.presentation.movie.discover.components.CategorySelectDialog
import io.silv.movie.presentation.movie.discover.components.MovieDiscoverTopBar
import io.silv.movie.presentation.movie.discover.components.SelectedPagingItemsGrid
import io.silv.movie.presentation.movie.view.MovieViewScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import org.koin.core.parameter.parametersOf

object MovieDiscoverScreen: Screen {

    private var genreSavedState: Genre? = null
    private var resourceSavedState: Resource? = null

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<MovieDiscoverScreenModel>() { parametersOf(genreSavedState, resourceSavedState) }
        val state by screenModel.state.collectAsStateWithLifecycle()
        val selectedPagingData by screenModel.pagingFlow.collectAsStateWithLifecycle()

        val pagingItems = selectedPagingData?.collectAsLazyPagingItems()

        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(screenModel) {
            snapshotFlow { state }.collectLatest {
                genreSavedState = it.selectedGenre
                resourceSavedState = it.selectedResource
            }
        }

        val onMovieLongClick = remember(screenModel) {
            { movie: Movie ->
                if (movie.favorite) {
                    screenModel.changeDialog(MovieDiscoverScreenModel.Dialog.RemoveMovie(movie))
                } else {
                    screenModel.toggleMovieFavorite(movie)
                }
            }
        }

        val onShowLongClick = remember(screenModel) {
            { show: TVShow ->
                if (show.favorite) {
                    screenModel.changeDialog(MovieDiscoverScreenModel.Dialog.RemoveShow(show))
                } else {
                    screenModel.toggleShowFavorite(show)
                }
            }
        }

        MovieDiscoverTabContent(
            setCurrentDialog = screenModel::changeDialog,
            contentByGenre = state.genreWithContent,
            selectedPagingItems = pagingItems,
            selectedGenre = { state.selectedGenre },
            selectedResource = { state.selectedResource },
            onResourceSelected = screenModel::onResourceSelected,
            clearFilters = screenModel::clearAllSelection,
            onMovieClick = { navigator.push(MovieViewScreen(it.id)) },
            onMovieLongClick = onMovieLongClick,
            onShowClick = {},
            onShowLongClick = onShowLongClick
        )

        val onDismissRequest = { screenModel.changeDialog(null) }
        when (val dialog = state.dialog) {
            MovieDiscoverScreenModel.Dialog.CategorySelect -> {
                CategorySelectDialog(
                    onDismissRequest = onDismissRequest,
                    selectedGenre = state.selectedGenre,
                    genres = when(state.selectedResource) {
                        Resource.Movie -> state.movieGenres
                        Resource.TVShow -> state.tvGenres
                        null -> state.genres
                    },
                    onGenreSelected = screenModel::onGenreSelected,
                    clearAllSelected = screenModel::clearAllSelection,
                )
            }
            is MovieDiscoverScreenModel.Dialog.RemoveMovie -> {
                RemoveEntryDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        screenModel.toggleMovieFavorite(dialog.movie)
                    },
                    entryToRemove = dialog.movie.title
                )
            }
            is MovieDiscoverScreenModel.Dialog.RemoveShow -> {
                RemoveEntryDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        screenModel.toggleShowFavorite(dialog.show)
                    },
                    entryToRemove = dialog.show.title
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
    selectedPagingItems: LazyPagingItems<StateFlow<Content>>?,
    setCurrentDialog: (MovieDiscoverScreenModel.Dialog?) -> Unit,
    contentByGenre: ImmutableList<Pair<Genre, ImmutableList<StateFlow<Content>>>>,
    onResourceSelected: (Resource?) -> Unit,
    onMovieLongClick: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
    clearFilters: () -> Unit,
    onShowLongClick: (TVShow) -> Unit,
    onShowClick: (TVShow) -> Unit
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
            label = ""
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
                    onMovieClick = onMovieClick,
                    onMovieLongClick = onMovieLongClick,
                    onShowClick = onShowClick,
                    onShowLongClick = onShowLongClick
                )
            } else {
                GenreItemsLists(
                    paddingValues = paddingValues,
                    hazeState = hazeState,
                    contentByGenre = contentByGenre,
                    onMovieLongClick = onMovieLongClick,
                    onMovieClick = onMovieClick,
                    onShowClick = onShowClick,
                    onShowLongClick = onShowLongClick
                )
            }
        }
    }
}

@Composable
fun GenreItemsLists(
    paddingValues: PaddingValues,
    hazeState: HazeState,
    contentByGenre: ImmutableList<Pair<Genre, ImmutableList<StateFlow<Content>>>>,
    onMovieLongClick: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onShowLongClick: (TVShow) -> Unit,
    onShowClick: (TVShow) -> Unit,
) {
    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier
            .fillMaxSize()
            .haze(hazeState)
    ) {
        items(
            items = contentByGenre,
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
                        key = { when(val c = it.value) {
                            is Content.CMovie -> c.movie.id
                            is Content.CShow -> c.show.id
                        } },
                    ) { contentStateFlow ->

                        val content by contentStateFlow.collectAsStateWithLifecycle()

                        Box(
                            Modifier
                                .width(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            when(val c = content) {
                                is Content.CMovie -> {
                                    EntryCompactGridItem(
                                        coverData = c.movie.toPoster(),
                                        coverAlpha = if (c.movie.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
                                        coverBadgeStart = { InLibraryBadge(enabled = c.movie.favorite) },
                                        onLongClick = { onMovieLongClick(c.movie) },
                                        onClick = { onMovieClick(c.movie) },
                                    )
                                }
                                is Content.CShow -> {
                                    EntryCompactGridItem(
                                        coverData = c.show.toPoster(),
                                        coverAlpha = if (c.show.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
                                        coverBadgeStart = { InLibraryBadge(enabled = c.show.favorite) },
                                        onLongClick = { onShowLongClick(c.show) },
                                        onClick = { onShowClick(c.show) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
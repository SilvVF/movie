package io.silv.movie.presentation.list.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.core_ui.components.PullRefresh
import io.silv.core_ui.util.colorClickable
import io.silv.movie.R
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.LocalContentInteractor
import io.silv.movie.presentation.LocalUser
import io.silv.movie.presentation.components.content.ContentListPosterGrid
import io.silv.movie.presentation.components.content.ContentListPosterList
import io.silv.movie.presentation.components.content.ContentPreviewDefaults
import io.silv.movie.presentation.components.dialog.FavoriteOptionsBottomSheet
import io.silv.movie.presentation.components.dialog.RemoveEntryDialog
import io.silv.movie.presentation.components.list.SpotifyTopBarLayout
import io.silv.movie.presentation.components.list.TitleWithProfilePicture
import io.silv.movie.presentation.components.list.rememberTopBarState
import io.silv.movie.presentation.content.screen.MovieViewScreen
import io.silv.movie.presentation.content.screen.TVViewScreen
import io.silv.movie.presentation.list.screenmodel.FavoritesListState
import io.silv.movie.presentation.list.screenmodel.FavoritesScreenModel
import io.silv.movie.presentation.list.screenmodel.FavoritesSortMode
import io.silv.movie.presentation.tabs.listNameSharedElement
import io.silv.movie.presentation.tabs.posterSharedElement

data object FavoritesViewScreen : Screen {

    @Composable
    override fun Content() {

        val contentInteractor = LocalContentInteractor.current
        val screenModel = koinScreenModel<FavoritesScreenModel>()
        val state by screenModel.state.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.currentOrThrow
        val changeDialog =
            remember { { dialog: FavoritesScreenModel.Dialog? -> screenModel.changeDialog(dialog) } }
        val toggleItemFavorite = remember {
            { contentItem: ContentItem -> contentInteractor.toggleFavorite(contentItem) }
        }

        FavoritesScreenContent(
            updateQuery = screenModel::updateQuery,
            onListOptionClick = { changeDialog(FavoritesScreenModel.Dialog.ListOptions) },
            listViewDisplayMode = { screenModel.listViewDisplayMode },
            updateListViewDisplayMode = screenModel::updateDisplayMode,
            query = screenModel.query,
            onClick = { item ->
                if (item.isMovie)
                    navigator.push(MovieViewScreen(item.contentId))
                else
                    navigator.push(TVViewScreen(item.contentId))
            },
            onLongClick = { item ->
                if (item.favorite) {
                    changeDialog(FavoritesScreenModel.Dialog.RemoveFromFavorites(item))
                } else {
                    toggleItemFavorite(item)
                }
            },
            onBackPressed = { navigator.pop() },
            onOptionsClick = {},
            changeSortMode = screenModel::setSortMode,
            refreshRecommendations = screenModel::refreshRecommendations,
            onRecommendationClick = { item ->
                if (item.isMovie)
                    navigator.push(MovieViewScreen(item.contentId))
                else
                    navigator.push(TVViewScreen(item.contentId))
            },
            onAddRecommendation = { contentInteractor.toggleFavorite(it) },
            onRecommendationLongClick = { contentInteractor.toggleFavorite(it) },
            refreshFavorites = screenModel::refreshFavoritesFromNetwork,
            state = state
        )
        val onDismissRequest = remember { { screenModel.changeDialog(null) } }
        when (val dialog = screenModel.currentDialog) {
            is FavoritesScreenModel.Dialog.ContentOptions -> {

            }
            FavoritesScreenModel.Dialog.ListOptions -> {
                FavoriteOptionsBottomSheet(
                    onDismissRequest = onDismissRequest,
                    onAddClick = { /*TODO*/ },
                    onShareClick = {}
                )
            }
            is FavoritesScreenModel.Dialog.RemoveFromFavorites -> {
                RemoveEntryDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = { toggleItemFavorite(dialog.item) },
                    entryToRemove = dialog.item.title
                )
            }
            null -> Unit
        }
    }
}

@Composable
private fun FavoritesScreenContent(
    updateQuery: (String) -> Unit,
    onListOptionClick:() -> Unit,
    listViewDisplayMode: () -> PosterDisplayMode,
    updateListViewDisplayMode: (PosterDisplayMode) -> Unit,
    query: String,
    onLongClick: (item: ContentItem) -> Unit,
    onClick: (item: ContentItem) -> Unit,
    onOptionsClick: (item: ContentItem) -> Unit,
    changeSortMode: (FavoritesSortMode) -> Unit,
    onRecommendationClick: (item: ContentItem) -> Unit,
    onRecommendationLongClick: (item: ContentItem) -> Unit,
    onAddRecommendation: (item: ContentItem) -> Unit,
    refreshRecommendations: () -> Unit,
    refreshFavorites: () -> Unit,
    onBackPressed: () -> Unit,
    state: FavoritesListState
) {
    val user = LocalUser.current

    PullRefresh(
        refreshing = state.refreshingFavorites,
        enabled = { user != null },
        onRefresh = { refreshFavorites() }
    ) {
        val lazyListState = rememberLazyListState()
        val lazyGridState = rememberLazyGridState()

        val topBarState = rememberTopBarState(
            if (listViewDisplayMode() is PosterDisplayMode.List)
                lazyListState
            else
                lazyGridState
        )

        val inOverlay by remember {
            derivedStateOf { topBarState.fraction > 0.2f && !topBarState.scrollableState.isScrollInProgress }
        }

        SpotifyTopBarLayout(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(topBarState.connection)
                .imePadding(),
            topBarState = topBarState,
            info = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    if (user != null) {
                        TitleWithProfilePicture(
                            user = user,
                            name = stringResource(id = R.string.favorites_top_bar_title),
                            description = "Your favorite movies and tv-shows",
                            textModifier = Modifier.listNameSharedElement(-1, inOverlay)
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.favorites_top_bar_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.listNameSharedElement(-1, inOverlay)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ListViewDisplayMode(
                            displayMode = listViewDisplayMode,
                            setDisplayMode = updateListViewDisplayMode
                        )
                    }
                }
            },
            search = {
                Row(Modifier.padding(horizontal = 18.dp)) {
                    Box(
                        Modifier
                            .weight(0.8f)
                            .fillMaxHeight()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                            .colorClickable {
                                topBarState.searching = !topBarState.searching
                            }
                            .padding(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Find in list", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        Modifier
                            .weight(0.2f)
                            .fillMaxHeight()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                            .colorClickable {

                            }
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sort", style = MaterialTheme.typography.labelLarge)
                    }
                }
            },
            poster = {
                ContentPreviewDefaults.LibraryContentPoster(
                    modifier = Modifier
                        .fillMaxHeight()
                        .posterSharedElement(-1, inOverlay)
                )
            },
            topAppBar = {
                PinnedTopBar(
                    onBackPressed = onBackPressed,
                    topBarState = topBarState,
                    onQueryChanged = updateQuery,
                    query = query,
                    user = LocalUser.current,
                    name = "Favorites"
                )
            },
            pinnedButton = {
                FilledIconButton(
                    onClick = {
                        onListOptionClick()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = null,
                    )
                }
            },
            snackbarHostState = remember { SnackbarHostState() }
        ) { paddingValues ->
            when (val mode = listViewDisplayMode()) {
                is PosterDisplayMode.Grid -> {
                    ContentListPosterGrid(
                        mode = mode,
                        lazyGridState = lazyGridState,
                        items = state.items,
                        onOptionsClick = onOptionsClick,
                        onLongClick = onLongClick,
                        onClick = onClick,
                        showFavorite = false,
                        paddingValues = paddingValues,
                        recommendations = state.recommendations,
                        onRecommendationLongClick = onRecommendationLongClick,
                        onRecommendationClick = onRecommendationClick,
                        onAddRecommendation = onAddRecommendation,
                        refreshingRecommendations = state.refreshingRecommendations,
                        onRefreshClick = refreshRecommendations,
                        isOwnerMe = true,
                    )
                }

                PosterDisplayMode.List -> {
                    ContentListPosterList(
                        items = state.items,
                        lazyListState = lazyListState,
                        onOptionsClick = onOptionsClick,
                        onLongClick = onLongClick,
                        onClick = onClick,
                        showFavorite = false,
                        paddingValues = paddingValues,
                        recommendations = state.recommendations,
                        onRecommendationLongClick = onRecommendationLongClick,
                        onRecommendationClick = onRecommendationClick,
                        onAddRecommendation = onAddRecommendation,
                        refreshingRecommendations = state.refreshingRecommendations,
                        onRefreshClick = refreshRecommendations,
                        isOwnerMe = true,
                    )
                }
            }
        }
    }
}
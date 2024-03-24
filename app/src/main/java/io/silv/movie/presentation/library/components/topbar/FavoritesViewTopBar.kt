package io.silv.movie.presentation.library.components.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import io.silv.core_ui.components.topbar.PosterLargeTopBar
import io.silv.core_ui.components.topbar.PosterTopBarState
import io.silv.core_ui.components.topbar.colors2
import io.silv.movie.R
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.library.components.ContentPreviewDefaults
import io.silv.movie.presentation.library.screenmodels.FavoritesSortMode

@Composable
fun FavoritesViewTopBar(
    state: PosterTopBarState,
    query: () -> String,
    changeQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    displayMode: () -> PosterDisplayMode,
    setDisplayMode: (PosterDisplayMode) -> Unit,
    onListOptionClicked: () -> Unit,
    sortModeProvider: () -> FavoritesSortMode,
    changeSortMode: (FavoritesSortMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
            .drawWithCache {
                onDrawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            colors = listOf(primary, background),
                            endY = size.height * 0.8f
                        ),
                        alpha = if (state.isKeyboardOpen) {
                            0f
                        } else {
                            1f - state.progress
                        }
                    )
                }
            },
    ) {
        PosterLargeTopBar(
            state = state,
            title = { Text(text = stringResource(id = R.string.favorites_top_bar_title)) },
            colors = TopAppBarDefaults.colors2(
                containerColor = Color.Transparent,
                scrolledContainerColor = primary.copy(alpha = 0.2f)
            ),
            navigationIcon = {
                PosterLargeTopBarDefaults.BackArrowIcon(
                    isKeyboardOpen = state.isKeyboardOpen
                )
            },
            actions = {
                PosterLargeTopBarDefaults.Actions(
                    displayMode = displayMode,
                    setDisplayMode = setDisplayMode,
                    onListOptionClicked = onListOptionClicked
                )
            },
            posterContent = {
                ContentPreviewDefaults.LibraryContentPoster(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxHeight()
                )
            },
            pinnedContent = {
                AnimatedVisibility(visible = !state.isKeyboardOpen) {
                    FavoritesFilterChips(
                        sortModeProvider = sortModeProvider,
                        changeSortMode = changeSortMode
                    )
                }
            }
        ) {
            PosterLargeTopBarDefaults.SearchInputField(
                query = query,
                onSearch = onSearch,
                changeQuery = changeQuery,
                placeholder = stringResource(id = R.string.search_placeholder, "Favorites")
            )
        }
    }
}


@Composable
fun FavoritesFilterChips(
    sortModeProvider: () -> FavoritesSortMode,
    changeSortMode: (FavoritesSortMode) -> Unit
) {
    val filters =
        remember {
            listOf(
                Triple(R.string.title, Icons.Filled.Title, FavoritesSortMode.Title),
                Triple(R.string.recently_added, Icons.Filled.NewReleases, FavoritesSortMode.RecentlyAdded),
                Triple(R.string.movies, Icons.Filled.Movie, FavoritesSortMode.Movie),
                Triple(R.string.shows, Icons.Filled.Tv, FavoritesSortMode.Show)
            )
        }

    val layoutDirection = LocalLayoutDirection.current

    val paddingHorizontal = with(LocalDensity.current) {
        TopAppBarDefaults.windowInsets.getLeft(this, layoutDirection).toDp() to
                TopAppBarDefaults.windowInsets.getRight(this, layoutDirection).toDp()
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = paddingHorizontal.first, end = paddingHorizontal.second),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val sortMode = sortModeProvider()
        filters.fastForEach { (tag, icon, type) ->
            item(key = tag) {
                ElevatedFilterChip(
                    modifier = Modifier.padding(4.dp),
                    selected = type == sortMode,
                    onClick = {
                        changeSortMode(type)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = icon.name,
                        )
                    },
                    label = {
                        Text(text = stringResource(tag))
                    },
                )
            }
        }
    }
}
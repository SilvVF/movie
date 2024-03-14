package io.silv.movie.presentation.library.view.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import io.silv.core_ui.components.topbar.PosterLargeTopBar
import io.silv.core_ui.components.topbar.PosterTopBarState
import io.silv.core_ui.components.topbar.colors2
import io.silv.core_ui.util.rememberDominantColor
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.library.components.ContentPreviewDefaults
import io.silv.movie.presentation.library.components.PosterLargeTopBarDefaults
import io.silv.movie.presentation.toPoster
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ListViewTopBar(
    state: PosterTopBarState,
    query: () -> String,
    changeQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    displayMode: () -> PosterDisplayMode,
    setDisplayMode: (PosterDisplayMode) -> Unit,
    onListOptionClicked: () -> Unit,
    contentListProvider: () -> ContentList,
    items: () -> ImmutableList<ContentItem>,
    sortModeProvider: () -> ListSortMode,
    changeSortMode: (ListSortMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content= items()
    val primary by rememberDominantColor(
        data = when  {
            content.isEmpty() -> null
            else -> content.first().toPoster()
        }
    )
    val background = MaterialTheme.colorScheme.background
    val primaryAnimated by animateColorAsState(
        targetValue = primary,
        label = ""
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
            .drawWithCache {
                onDrawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            colors = if (items().isEmpty()) {
                                listOf(primaryAnimated, background)
                            } else {
                                listOf(primaryAnimated, background)
                            },
                            endY = size.height * 0.8f
                        ),
                        alpha = if(state.isKeyboardOpen) 0f else 1f - state.progress
                    )
                }
            },
    ) {
        val contentList = contentListProvider()
        PosterLargeTopBar(
            state = state,
            title = { Text(text = contentList.name) },
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
                val posterModifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxHeight()
                when {
                    content.isEmpty() -> {
                        ContentPreviewDefaults.PlaceholderPoster(
                            modifier = posterModifier
                        )
                    }
                    content.size < 4 -> {
                        ContentPreviewDefaults.SingleItemPoster(
                            item = content.first(),
                            modifier = posterModifier
                        )
                    }

                    else -> {
                        ContentPreviewDefaults.MultiItemPoster(
                            modifier = posterModifier,
                            items = content
                        )
                    }
                }
            },
            pinnedContent = {
                AnimatedVisibility(visible = !state.isKeyboardOpen) {
                    ListFilterChips(
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
                placeholder = "Search ${contentList.name}"
            )
        }
    }
}



@Composable
fun ListFilterChips(
    sortModeProvider: () -> ListSortMode,
    changeSortMode: (ListSortMode) -> Unit
) {
    val filters =
        remember {
            listOf(
                Triple("Title", Icons.Filled.Title, ListSortMode.Title),
                Triple("Recently Added", Icons.Filled.NewReleases, ListSortMode.RecentlyAdded),
                Triple("Movies", Icons.Filled.Movie, ListSortMode.Movie),
                Triple("Shows", Icons.Filled.Tv, ListSortMode.Show)
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
                        Text(text = tag)
                    },
                )
            }
        }
    }
}


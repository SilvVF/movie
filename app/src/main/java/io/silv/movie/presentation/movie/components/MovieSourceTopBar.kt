package io.silv.movie.presentation.movie.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import io.silv.core_ui.components.SearchBarInputField
import io.silv.core_ui.components.SearchLargeTopBar
import io.silv.core_ui.components.TooltipIconButton
import io.silv.core_ui.components.colors2
import io.silv.data.MoviePagedType
import io.silv.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.movie.MovieActions
import io.silv.movie.presentation.movie.Resource

@Composable
fun MovieTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    resource: () -> Resource,
    query: () -> String,
    listing: () -> MoviePagedType,
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
    Column(
        modifier = modifier.fillMaxWidth().wrapContentSize(),
    ) {
        SearchLargeTopBar(
            title = { Text("TMDB") },
            scrollBehavior = scrollBehavior,
            colors = colors,
            actions = {
                ResourceFilterChips(
                    changeResourceType = { actions.changeResource(it) },
                    selected = resource
                )
                var dropDownVisible by remember { mutableStateOf(false) }

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
                query = query(),
                placeholder = {
                    Text(remember(resource) { "Search for ${resource}..." })
                },
                onQueryChange = { actions.changeQuery(it) },
                focusRequester = focusRequester,
                onSearch = {
                    actions.onSearch(it)
                    keyboardController?.hide()
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(visible = query().isNotEmpty()) {
                            IconButton(onClick = { actions.changeQuery("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = null
                                )
                            }
                        }
                        IconButton(onClick = { actions.onSearch(query()) }) {
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
                selected = listing(),
                query = query(),
                changeMovePagesType = {
                    actions.changeCategory(it)
                }
            )
        }
    }
}

@Composable
fun RowScope.ResourceFilterChips(
    changeResourceType: (Resource) -> Unit,
    selected:() -> Resource,
) {
    val contentColor = LocalContentColor.current
    TooltipIconButton(
        onClick = { changeResourceType(Resource.Movie) },
        imageVector = Icons.Filled.Movie,
        tooltip = "Movies",
        tint = animateColorAsState(
            targetValue = if (selected() == Resource.Movie) {
                MaterialTheme.colorScheme.primary
            } else {
                contentColor
            },
            label = ""
        ).value
    )
    TooltipIconButton(
        onClick = { changeResourceType(Resource.TVShow) },
        imageVector = Icons.Filled.Tv,
        tooltip = "TV Shows",
        contentDescription = null,
        tint = animateColorAsState(
            targetValue = if (selected() == Resource.TVShow) {
                MaterialTheme.colorScheme.primary
            } else {
                contentColor
            },
            label = ""
        ).value
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
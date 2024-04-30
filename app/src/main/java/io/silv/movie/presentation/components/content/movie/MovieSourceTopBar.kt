package io.silv.movie.presentation.components.content.movie

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.core_ui.components.TMDBLogo
import io.silv.core_ui.components.TooltipIconButton
import io.silv.core_ui.components.topbar.SearchBarInputField
import io.silv.core_ui.components.topbar.SearchLargeTopBar
import io.silv.core_ui.components.topbar.colors2
import io.silv.movie.R
import io.silv.movie.data.content.ContentPagedType
import io.silv.movie.data.prefrences.PosterDisplayMode

@Composable
fun ContentBrowseTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    query: () -> String,
    listing: () -> ContentPagedType,
    displayMode: () -> PosterDisplayMode,
    changePagedType: (ContentPagedType) -> Unit,
    changeResourceType: () -> Unit,
    setDisplayMode: (PosterDisplayMode) -> Unit,
    changeQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    onFilterClick: () -> Unit,
    isMovie: Boolean,
) {
    val barFullyCollapsed by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction == 1f }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(),
    ) {
        SearchLargeTopBar(
            title = {
                TMDBLogo(
                    modifier = Modifier
                        .padding(vertical = 18.dp)
                        .height(24.dp),
                    contentScale = ContentScale.Fit
                )
            },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.colors2(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = if (barFullyCollapsed)
                    Color.Transparent
                else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            navigationIcon = {
                val navigator = LocalNavigator.current
                if (navigator?.canPop == true) {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            },
            actions = {
                ResourceFilterChips(
                    changeResourceType = changeResourceType,
                    isMovie = isMovie
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
                                        onClick = { setDisplayMode(it) }
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
                                onClick = { setDisplayMode(it) }
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
                        tooltip = stringResource(id = R.string.display_mode)
                    )
                }
            },
            pinnedContent = {
                MovieFilterChips(
                    selected = listing(),
                    changeMovePagesType = { changePagedType(it) },
                    onFilterClick = onFilterClick
                )
            }
        ) {
            val focusManager = LocalFocusManager.current

            SearchBarInputField(
                query = query(),
                placeholder = {
                    Text(
                        text = stringResource(
                            id = R.string.search_browse_top_bar_hint,
                            if (isMovie) "Movies" else "TV-Shows"
                        )
                    )
                },
                onQueryChange = { changeQuery(it) },
                onSearch = {
                    onSearch(it)
                    focusManager.clearFocus(false)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(visible = query().isNotEmpty()) {
                            IconButton(onClick = { changeQuery("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = null
                                )
                            }
                        }
                        IconButton(onClick = { onSearch(query()) }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(id = R.string.search)
                            )
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
fun RowScope.ResourceFilterChips(
    changeResourceType: () -> Unit,
    isMovie: Boolean,
) {
    val contentColor = LocalContentColor.current
    TooltipIconButton(
        onClick = {
            if (!isMovie) {
                changeResourceType()
            }
        },
        imageVector = Icons.Filled.Movie,
        tooltip = stringResource(id = R.string.movies),
        tint = animateColorAsState(
            targetValue = if (isMovie) {
                MaterialTheme.colorScheme.primary
            } else {
                contentColor
            },
            label = ""
        ).value
    )
    TooltipIconButton(
        onClick = {
            if (isMovie) {
                changeResourceType()
            }
        },
        imageVector = Icons.Filled.Tv,
        tooltip = stringResource(id = R.string.shows),
        contentDescription = null,
        tint = animateColorAsState(
            targetValue = if (!isMovie) {
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
    changeMovePagesType: (ContentPagedType) -> Unit,
    selected: ContentPagedType,
    onFilterClick: () -> Unit
) {
    val filters =
        remember {
            listOf(
                Triple(R.string.popular, Icons.Filled.Whatshot, ContentPagedType.Default.Popular),
                Triple(R.string.top_rated, Icons.Filled.AutoAwesome, ContentPagedType.Default.TopRated),
                Triple(R.string.upcoming, Icons.Filled.NewReleases, ContentPagedType.Default.Upcoming)
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
        filters.fastForEach { (tag, icon, type) ->
            item(key = tag) {
                ElevatedFilterChip(
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
                        Text(text = stringResource(tag))
                    },
                )
            }
        }
        item(key = "filter") {
            ElevatedFilterChip(
                modifier = Modifier.padding(4.dp),
                selected = selected is ContentPagedType.Search || selected is ContentPagedType.Discover,
                onClick = onFilterClick,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = stringResource(id = R.string.filter),
                    )
                },
                label = {
                    Text(text = stringResource(id = R.string.filter))
                },
            )
        }
    }
}
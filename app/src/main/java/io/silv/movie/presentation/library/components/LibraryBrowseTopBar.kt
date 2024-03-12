package io.silv.movie.presentation.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import io.silv.core_ui.components.SearchBarInputField
import io.silv.core_ui.components.SearchLargeTopBar
import io.silv.core_ui.components.TooltipIconButton
import io.silv.core_ui.components.colors2
import io.silv.movie.presentation.library.browse.LibrarySortMode

@Composable
fun LibraryBrowseTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    query: () -> String,
    sortModeProvider: () -> LibrarySortMode,
    changeSortMode: (LibrarySortMode) -> Unit,
    isListMode: () -> Boolean,
    setListMode: (Boolean) -> Unit,
    changeQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    createListClicked: () -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(),
    ) {
        SearchLargeTopBar(
            title = {
                Text(text = "Your Library")
            },
            scrollBehavior = scrollBehavior,
            colors = colors,
            actions = {
                var dropDownVisible by remember { mutableStateOf(false) }
                TooltipIconButton(
                    onClick = createListClicked,
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tooltip = "Create list"
                )
                Box(contentAlignment = Alignment.BottomCenter) {
                    val isList = isListMode()
                    DropdownMenu(
                        expanded = dropDownVisible,
                        onDismissRequest = { dropDownVisible = false }
                    ) {
                        DropdownMenuItem(
                            trailingIcon = {
                                RadioButton(
                                    selected = isList,
                                    onClick = { setListMode(true) }
                                )
                            },
                            text = {
                                Text("List")
                            },
                            onClick = { setListMode(true) }
                        )
                        DropdownMenuItem(
                            trailingIcon = {
                                RadioButton(
                                    selected = !isList,
                                    onClick = { setListMode(false) }
                                )
                            },
                            text = {
                                Text("Grid")
                            },
                            onClick = { setListMode(false) }
                        )
                    }
                    TooltipIconButton(
                        onClick = { dropDownVisible = true },
                        imageVector = if(isList) {
                            Icons.AutoMirrored.Filled.List
                        } else{
                            Icons.Filled.GridView
                        },
                        contentDescription = null,
                        tooltip = "Display Mode"
                    )
                }
            },
        ) {
            val focusManager = LocalFocusManager.current

            SearchBarInputField(
                query = query(),
                placeholder = {
                    Text( "Search your library...")
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
            LibraryFilterChips(sortModeProvider, changeSortMode)
        }
    }
}

@Composable
fun LibraryFilterChips(
    sortModeProvider: () -> LibrarySortMode,
    changeSortMode: (LibrarySortMode) -> Unit
) {
    val filters =
        remember {
            listOf(
                Triple("Title", Icons.Filled.Title, LibrarySortMode.Title),
                Triple("Recently Added", Icons.Filled.NewReleases, LibrarySortMode.RecentlyAdded),
                Triple("Count", Icons.Filled.Numbers, LibrarySortMode.Count)
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
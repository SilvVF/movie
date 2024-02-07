package io.silv.movie.presentation.movie

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import io.silv.core_ui.components.SearchBarInputField
import io.silv.core_ui.components.SearchLargeTopBar
import io.silv.core_ui.components.colors2
import io.silv.core_ui.components.toPoster
import io.silv.data.Movie
import io.silv.data.MoviePagedType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

object MovieScreen: Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<MovieScreenModel>()

        val state by screenModel.state.collectAsStateWithLifecycle()

        MovieScreenContent(
            state = state,
            pagingFlowFlow = { screenModel.moviePagerFlowFlow }
        )
    }
}

@Composable
private fun MovieScreenContent(
    state: MovieState,
    pagingFlowFlow: () -> StateFlow<Flow<PagingData<StateFlow<Movie>>>>
) {

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var query by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                Column {
                    val colors = TopAppBarDefaults.colors2()
                    val colorTransitionFraction = scrollBehavior.state.collapsedFraction
                    val appBarContainerColor by rememberUpdatedState(
                        colors.containerColor(colorTransitionFraction)
                    )
                    SearchLargeTopBar(
                        title = { Text("TMDB") },
                        scrollBehavior = scrollBehavior,
                        actions = {
                            IconButton(onClick = { /*TODO*/ }) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    contentDescription = null
                                )
                            }
                        }
                    ) {
                        val focusRequester = remember { FocusRequester() }

                        SearchBarInputField(
                            query = query,
                            placeholder = { Text("Search for movies...") },
                            onQueryChange = { query = it },
                            focusRequester = focusRequester,
                            onSearch = {},
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AnimatedVisibility(visible = query.isNotEmpty()) {
                                        IconButton(
                                            onClick = { query = "" }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Clear,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {}
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
                    Surface(color = appBarContainerColor) {
                        MovieFilterChips(
                            selected = state.listing,
                            query = query,
                            changeMovePagesType = {}
                        )
                    }
                }
            },
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { paddingValues ->
            MoviePagedGrid(
                paddingValues = paddingValues,
                pagingFlowFlow = pagingFlowFlow
            )
        }
    }
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
                    modifier = Modifier.padding(2.dp),
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
fun MoviePagedGrid(
    pagingFlowFlow: () -> StateFlow<Flow<PagingData<StateFlow<Movie>>>>,
    paddingValues: PaddingValues,
) {
    val pagingFlow by pagingFlowFlow().collectAsStateWithLifecycle()
    val pagingData = pagingFlow.collectAsLazyPagingItems()

    if (pagingData.loadState.refresh is LoadState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (pagingData.loadState.refresh is LoadState.Error) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("error")
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = paddingValues,
        modifier = Modifier.padding(top = 12.dp)
    ) {
        items(
            count = pagingData.itemCount,
            key = pagingData.itemKey { it.value.id },
            contentType = pagingData.itemContentType()
        ) {
            val movie = pagingData[it]?.collectAsStateWithLifecycle()

            val poster by remember(movie) {
                derivedStateOf { movie?.value?.toPoster() }
            }

            poster?.Render()
        }
        if (pagingData.loadState.append is LoadState.Loading) {
            item(
                key = "loading-append"
            ) {
                CircularProgressIndicator()
            }
        }
        if (pagingData.loadState.append is LoadState.Error) {
            item(
                key = "error-append"
            ) {
                Text("error")
            }
        }
    }
}
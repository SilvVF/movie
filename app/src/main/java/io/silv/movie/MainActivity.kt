@file:OptIn(ExperimentalMaterial3Api::class)

package io.silv.movie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.movie.domain.movie.MoviePagedType
import io.silv.movie.presentation.movie.MovieViewModel
import io.silv.movie.ui.components.Poster
import io.silv.movie.ui.components.SearchBarInputField
import io.silv.movie.ui.components.SearchLargeTopBar
import io.silv.movie.ui.components.colors2
import io.silv.movie.ui.theme.MovieTheme
import org.koin.androidx.viewmodel.ext.android.getViewModel





class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)


        setContent {
            MovieTheme {

                val viewModel = getViewModel<MovieViewModel>()

                val moviePagingData = viewModel.moviePagedData.collectAsLazyPagingItems()
                val selectedPagedType by viewModel.selectedPageType.collectAsStateWithLifecycle()

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
                                            Icon(imageVector = Icons.Filled.FilterList, contentDescription = null)
                                        }
                                    }
                                ) {
                                    val focusRequester = remember { FocusRequester() }

                                    SearchBarInputField(
                                        query = query,
                                        placeholder = { Text("Search for movies...")},
                                        onQueryChange = { query = it },
                                        focusRequester = focusRequester,
                                        onSearch = { viewModel.changePagingData(MoviePagedType.Filter(it)) },
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
                                                    onClick = {
                                                        viewModel.changePagingData(MoviePagedType.Filter(query))
                                                    }
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
                                    MovieFilterChips(selectedPagedType) {
                                        if (it is MoviePagedType.Filter) {
                                            viewModel.changePagingData(MoviePagedType.Filter(query))
                                        } else {
                                            viewModel.changePagingData(it)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                    ) { paddingValues ->
                        MoviePagedGrid(
                            moviePagingData = moviePagingData,
                            paddingValues = paddingValues
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieFilterChips(
    selected: MoviePagedType,
    changeMovePagesType: (MoviePagedType) -> Unit,
) {
    val filters =
        remember {
            listOf(
                Triple("Popular", Icons.Filled.Whatshot, MoviePagedType.Popular),
                Triple("Top Rated", Icons.Outlined.AutoAwesome, MoviePagedType.TopRated),
                Triple("Upcoming", Icons.Filled.NewReleases, MoviePagedType.Upcoming),
                Triple("Filter", Icons.Filled.FilterList, MoviePagedType.Filter("")),
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
    moviePagingData: LazyPagingItems<Poster>,
    paddingValues: PaddingValues,
) {
    if (moviePagingData.loadState.refresh is LoadState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (moviePagingData.loadState.refresh is LoadState.Error) {
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
            count = moviePagingData.itemCount,
            key = moviePagingData.itemKey { it.id },
            contentType = moviePagingData.itemContentType()
        ) {
            moviePagingData[it]?.Render()
        }
        if (moviePagingData.loadState.append is LoadState.Loading) {
            item(
                key = "loading-append"
            ) {
                CircularProgressIndicator()
            }
        }
        if (moviePagingData.loadState.append is LoadState.Error) {
            item(
                key = "error-append"
            ) {
                Text("error")
            }
        }
    }
}
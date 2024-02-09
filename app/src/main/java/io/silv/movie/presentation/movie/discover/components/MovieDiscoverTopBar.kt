package io.silv.movie.presentation.movie.discover.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.core_ui.components.MediumTopAppBarHideItems
import io.silv.core_ui.components.TMDBLogo
import io.silv.core_ui.components.colors2
import io.silv.data.movie.model.Genre
import io.silv.movie.presentation.movie.browse.MovieScreen
import io.silv.movie.presentation.movie.browse.Resource
import io.silv.movie.presentation.movie.discover.MovieDiscoverScreenModel

@Composable
fun MovieDiscoverTopBar(
    modifier: Modifier = Modifier,
    selectedGenre: () -> Genre?,
    selectedResource: () -> Resource?,
    setCurrentDialog: (MovieDiscoverScreenModel.Dialog?) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val navigator = LocalNavigator.current
    MediumTopAppBarHideItems(
        title = {
            TMDBLogo(modifier = Modifier.height(24.dp))
        },
        actions = {
            IconButton(onClick = { navigator?.push(MovieScreen()) }) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null
                )
            }
        },
        extraContent = {
            DiscoverMovieFilters(
                modifier = Modifier.fillMaxWidth(),
                selectedGenre = selectedGenre,
                selectedResource = selectedResource,
                onClearClick = { /*TODO*/ },
                onResourceSelected = {},
                onCategoryClick = {
                    setCurrentDialog(MovieDiscoverScreenModel.Dialog.CategorySelect)
                }
            )
        },
        navigationIcon = {

        },
        colors = TopAppBarDefaults.colors2(Color.Transparent, scrolledContainerColor = Color.Transparent),
        modifier = modifier,
        scrollBehavior = scrollBehavior
    )
}

@Composable
fun DiscoverMovieFilters(
    modifier: Modifier = Modifier,
    selectedGenre: () -> Genre?,
    selectedResource: () -> Resource?,
    onClearClick: () -> Unit,
    onResourceSelected: (Resource?) -> Unit,
    onCategoryClick: () -> Unit,
) {
    val category = selectedGenre()
    val resource = selectedResource()
    LazyRow(
        modifier = modifier
    ) {
        if (category != null && resource != null) {
            item(key = "clear-button") {
                ElevatedAssistChip(
                    onClick =  onClearClick,
                    label = { Text("Clear") },
                    modifier = Modifier.padding(2.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "clear"
                        )
                    }
                )
            }
        }
        if (resource == Resource.TVShow || resource == null) {
            item(key = "resource-tv") {
                ElevatedFilterChip(
                    selected = resource == Resource.TVShow,
                    onClick =  {
                        onResourceSelected(Resource.TVShow.takeIf { resource != it })
                    },
                    label = { Text("TV Shows") },
                    modifier = Modifier.padding(2.dp),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Tv,
                            contentDescription = "Tv"
                        )
                    }
                )
            }
        }
        if (resource == Resource.Movie || resource == null) {
            item(key = "resource-movie") {
                ElevatedFilterChip(
                    selected = resource == Resource.Movie,
                    modifier = Modifier.padding(2.dp),
                    onClick =  {
                        onResourceSelected(Resource.Movie.takeIf { resource != it })
                    },
                    label = { Text("Movies") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = "movie"
                        )
                    }
                )
            }
        }
        item(key = "category-filter") {
            ElevatedFilterChip(
                selected = category != null,
                onClick =  onCategoryClick,
                modifier = Modifier.padding(2.dp),
                label = {
                    Text(
                       text = category?.name ?: "Categories"
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "category"
                    )
                }
            )
        }
    }
}
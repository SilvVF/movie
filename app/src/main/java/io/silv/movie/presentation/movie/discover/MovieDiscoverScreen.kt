package io.silv.movie.presentation.movie.discover

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.movie.presentation.movie.discover.components.CategorySelectDialog
import io.silv.movie.presentation.movie.discover.components.MovieDiscoverTopBar
import kotlinx.collections.immutable.persistentListOf

object MovieDiscoverTab: Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = "Discover",
            icon = rememberVectorPainter(image = Icons.Filled.FilterNone)
        )

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<MovieDiscoverScreenModel>()
        val state by screenModel.state.collectAsStateWithLifecycle()

        MovieDiscoverTabContent(
            setCurrentDialog = screenModel::changeDialog
        )

        val onDismissRequest = { screenModel.changeDialog(null) }
        when (state.dialog) {
            MovieDiscoverScreenModel.Dialog.CategorySelect -> {
                CategorySelectDialog(
                    onDismissRequest = onDismissRequest,
                    selectedGenres = persistentListOf(),
                    genres = state.genres,
                    onGenreSelected = {},
                    clearAllSelected = {}
                )
            }
            null -> Unit
        }
    }
}


@Composable
fun MovieDiscoverTabContent(
    setCurrentDialog: (MovieDiscoverScreenModel.Dialog?) -> Unit,
) {
    val hazeState = remember { HazeState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
           MovieDiscoverTopBar(
               modifier = Modifier.fillMaxWidth()
                   .hazeChild(
                       state = hazeState,
                       style = HazeDefaults.style(
                           backgroundColor = MaterialTheme.colorScheme.background
                       )
               ),
               setCurrentDialog = setCurrentDialog,
               scrollBehavior = scrollBehavior,
               selectedGenre = { null },
               selectedResource = { null }
           )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.haze(state = hazeState)
        ) {
            items(100) {
                Text(text = "Item Move title and poster go here $it", modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp))
            }
        }
    }
}
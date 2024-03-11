package io.silv.movie.presentation.library

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
import io.silv.movie.presentation.CollectEventsWithLifecycle
import io.silv.movie.presentation.library.components.LibraryBrowseTopBar
import io.silv.movie.presentation.library.components.LibraryGridView
import io.silv.movie.presentation.library.components.LibraryListView


object LibraryTab: Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = "Library",
            icon = rememberVectorPainter(image = Icons.AutoMirrored.Rounded.LibraryBooks)
        )

    @Composable
    override fun Content() {
        Navigator(
            LibraryScreen()
        ) {
            FadeTransition(it)
        }
    }
}

class LibraryScreen: Screen  {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<LibraryScreenModel>()
        val state by screenModel.state.collectAsStateWithLifecycle()
        val listCount by screenModel.listCount.collectAsStateWithLifecycle()

        val listCreateScreen = remember(listCount) { ListCreateScreen(listCount) }

        val screenResultLauncher = rememberScreenWithResultLauncher(
            screen = listCreateScreen
        ) { result ->
            screenModel.createList(result.name)
        }

        val context = LocalContext.current

        CollectEventsWithLifecycle(screenModel) {
            when (it) {
                is LibraryEvent.ListCreated -> {
                    Toast.makeText(context, "created list ${it.id}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        LibraryStandardScreenContent(
            query = { screenModel.query },
            sortModeProvider = { screenModel.sortMode },
            listModeProvider = { screenModel.displayInList },
            changeQuery = screenModel::updateQuery,
            setListMode = screenModel::updateListMode,
            changeSortMode = screenModel::updateSortMode,
            createListClicked = { screenResultLauncher.launch() },
            state = state
        )
    }
}

@Composable
private fun LibraryStandardScreenContent(
    query: () -> String,
    sortModeProvider: () -> LibrarySortMode,
    listModeProvider: () -> Boolean,
    setListMode: (Boolean) -> Unit,
    changeQuery: (query: String) -> Unit,
    changeSortMode: (mode: LibrarySortMode) -> Unit,
    createListClicked: () -> Unit,
    state: LibraryState
) {

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val snackBarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            LibraryBrowseTopBar(
                modifier = Modifier.hazeChild(hazeState),
                isListMode = listModeProvider,
                changeQuery = changeQuery,
                onSearch = changeQuery,
                query = query,
                setListMode = setListMode,
                changeSortMode = changeSortMode,
                sortModeProvider =  sortModeProvider,
                scrollBehavior = scrollBehavior,
                createListClicked = createListClicked,
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
            .exclude(WindowInsets.systemBars),
        snackbarHost = {
            SnackbarHost(snackBarHostState)
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        Crossfade(
            targetState = listModeProvider(),
            label = "display-mode-crossfade"
        ) { isList ->
            if (isList) {
                LibraryListView(
                    paddingValues = paddingValues,
                    state = state,
                    modifier = Modifier.haze(
                        hazeState,
                        HazeDefaults.style(MaterialTheme.colorScheme.background)
                    )
                )
            } else {
                LibraryGridView(
                    paddingValues = paddingValues,
                    state = state,
                    modifier = Modifier.haze(
                        hazeState,
                        HazeDefaults.style(MaterialTheme.colorScheme.background)
                    )
                )
            }
        }
    }
}




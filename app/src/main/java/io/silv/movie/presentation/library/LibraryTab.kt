package io.silv.movie.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.ItemCover
import io.silv.core_ui.components.PosterData
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListItem
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.lists.GetFavoritesList
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.library.components.LibraryBrowseTopBar
import io.silv.movie.presentation.toPoster
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class LibraryScreenModel(
    private val contentListRepository: ContentListRepository,
    getFavoritesList: GetFavoritesList
): StateScreenModel<LibraryState>(LibraryState()) {

    var query by mutableStateOf("")
        private set

    private val queryFlow = snapshotFlow { query }

    init {
        queryFlow.flatMapLatest { query ->
            contentListRepository.observeLibraryItems(query)
        }
            .onEach {

                val grouped = it.groupBy { it.list }
                    .mapValues { (list, items) ->
                        items.toImmutableList()
                    }
                    .toList()
                    .toImmutableList()

                mutableState.update { state ->
                    state.copy(
                        contentLists = grouped
                    )
                }
            }
            .launchIn(screenModelScope)

        getFavoritesList.subscribe()
            .onEach { posters ->
                mutableState.update { state ->
                    state.copy(
                        favorites = posters.toImmutableList()
                    )
                }
            }
            .launchIn(screenModelScope)
    }
}

data class LibraryState(
    val contentLists: ImmutableList<Pair<ContentList, ImmutableList<ContentListItem>>> = persistentListOf(),
    val favorites: ImmutableList<PosterData> = persistentListOf()
)

object LibraryTab: Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = "Library",
            icon = rememberVectorPainter(image = Icons.AutoMirrored.Rounded.LibraryBooks)
        )

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<LibraryScreenModel>()
        val state by screenModel.state.collectAsStateWithLifecycle()

        LibraryStandardScreenContent(
            query = { screenModel.query },
            state = state
        )
    }
}

private val ListItemHeight = 72.dp

@Composable
fun LibraryStandardScreenContent(
    query: () -> String,
    state: LibraryState
) {

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val snackBarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            LibraryBrowseTopBar(
                modifier = Modifier.hazeChild(hazeState),
                displayMode = { PosterDisplayMode.default },
                changeQuery = {},
                onSearch = {},
                query = query,
                setDisplayMode = {},
                scrollBehavior = scrollBehavior,
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
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.haze(
                state = hazeState,
                style = HazeDefaults.style(MaterialTheme.colorScheme.background),
            ),
        ) {
            item {
                ContentListPreview(
                    modifier = Modifier.padding(8.dp),
                    cover = {
                        val primary =   MaterialTheme.colorScheme.primary
                        val secondary = MaterialTheme.colorScheme.secondary
                        val tertiary = MaterialTheme.colorScheme.tertiary
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .aspectRatio(1f)
                                .drawWithCache {
                                    onDrawBehind {
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                listOf(
                                                    primary,
                                                    secondary,
                                                    tertiary
                                                )
                                            )
                                        )
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(42.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    },
                    name = "Library Content",
                    count = state.favorites.size
                )
            }
            state.contentLists.fastForEach { (list, items) ->
                item(
                    key = list.id
                ) {
                    ContentListPreview(
                        modifier = Modifier.padding(8.dp),
                        cover = {
                            if (items.size < 4) {
                                ContentListPreviewDefaults.SingleItemPoster(
                                    modifier = Modifier.fillMaxSize(),
                                    item = items.first()
                                )
                            } else {
                                ContentListPreviewDefaults.MultiItemPoster(
                                    modifier = Modifier.fillMaxSize(),
                                    items = items
                                )
                            }
                        },
                        name = list.name,
                        count = when(items.first()) {
                            is ContentListItem.PlaceHolder -> 0
                            is ContentListItem.Item -> items.size
                        }
                    )
                }
            }
        }
    }
}

object ContentListPreviewDefaults {

    @Composable
    private fun PlaceholderPoster(
        modifier: Modifier
    ) {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            color = Color.DarkGray
                        )
                    }
                }
        ) {
            Icon(
                imageVector = Icons.Filled.MovieFilter,
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .align(Alignment.Center)
            )
        }
    }

    @Composable
    fun MultiItemPoster(
        modifier: Modifier,
        items: ImmutableList<ContentListItem>
    ) {
        val trimmed = remember(items) {
            items.filterIsInstance<ContentListItem.Item>().take(4)
        }

        FlowRow(
            maxItemsInEachRow = 2,
            modifier = modifier.aspectRatio(1f)
        ) {
            trimmed.fastForEach {
                ItemCover.Square(
                    modifier = Modifier
                        .height(ListItemHeight / 2)
                        .weight(1f),
                    shape = RectangleShape,
                    data = remember(it) { it.toPoster() }
                )
            }
        }
    }

    @Composable
    fun SingleItemPoster(
        modifier: Modifier,
        item: ContentListItem
    ) {
        when (item) {
            is ContentListItem.Item -> {
                ItemCover.Square(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                    data = remember(item) { item.toPoster() }
                )
            }
            is ContentListItem.PlaceHolder -> PlaceholderPoster(modifier = modifier)
        }
    }
}

@Composable
private fun ContentListPreview(
    cover: @Composable () -> Unit,
    name: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.heightIn(0.dp, ListItemHeight),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(0.2f, fill = false)
                .fillMaxHeight()
        ) {
            cover()
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(0.8f, true)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (count == 0) "No items in list" else "$count items in list",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.graphicsLayer { alpha = 0.78f }
            )
        }
    }
}

@Composable
private fun LibraryListItem(
    list: ContentList,
    items: ImmutableList<ContentListItem>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (items.size >= 4) {

        } else if (items.isNotEmpty() && items.first() !is ContentListItem.PlaceHolder) {
            ItemCover.Square(
                modifier = Modifier.weight(0.2f, false),
                data = remember(items) { (items.first() as ContentListItem.Item).toPoster() }
            )
        } else {

        }

    }
}


package io.silv.movie.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.core_ui.components.PageLoadingIndicator
import io.silv.movie.R
import io.silv.movie.data.model.ContentItem
import io.silv.movie.koin4ScreenModel
import io.silv.movie.presentation.LocalContentInteractor
import io.silv.movie.presentation.components.content.ContentListItem
import io.silv.movie.presentation.components.dialog.RemoveEntryDialog
import io.silv.movie.presentation.screenmodel.ListAddScreenModel
import io.silv.movie.presentation.screenmodel.ListAddState
import io.silv.movie.presentation.toPoster
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.parameter.parametersOf
import kotlin.math.absoluteValue

data class ListAddScreen(
    val listId: Long
) : Screen {

    override val key: ScreenKey = "add_$listId"

    @Composable
    override fun Content() {
        val contentInteractor = LocalContentInteractor.current
        val screenModel = koin4ScreenModel<ListAddScreenModel> { parametersOf(listId) }
        val state by screenModel.state.collectAsStateWithLifecycle()
        val contentPagingItems = screenModel.contentPagingFlow.collectAsLazyPagingItems()
        val navigator = LocalNavigator.currentOrThrow
        val toggleFavorite = remember {
            { contentItem: ContentItem -> contentInteractor.toggleFavorite(contentItem) }
        }
        val changeDialog = remember {
            { dialog: ListAddScreenModel.Dialog? -> screenModel.changeDialog(dialog) }
        }

        val snackbarHostState = remember { SnackbarHostState() }

        when (val s = state) {
            is ListAddState.Error -> {}
            ListAddState.Loading -> {}
            is ListAddState.Success -> {
                SuccessScreenContent(
                    query = screenModel.query,
                    onClick = {
                        if (it.isMovie)
                            navigator.push(MovieViewScreen(it.contentId))
                        else
                            navigator.push(TVViewScreen(it.contentId))
                    },
                    onAddClick = {
                        contentInteractor.addToList(listId, it)
                    },
                    onLongClick = {
                        if (it.favorite) {
                            changeDialog(ListAddScreenModel.Dialog.RemoveFromFavorites(it))
                        } else {
                            toggleFavorite(it)
                        }
                    },
                    snackbarHostState = snackbarHostState,
                    contentPagingItems = contentPagingItems,
                    changeQuery = screenModel::updateQuery,
                    changePagingItems = screenModel::changePagingItems,
                    state = s
                )
                val onDismissRequest = remember { { screenModel.changeDialog(null) } }
                when (val dialog = s.dialog) {
                    is ListAddScreenModel.Dialog.RemoveFromFavorites -> {
                        RemoveEntryDialog(
                            onDismissRequest = onDismissRequest,
                            onConfirm = { toggleFavorite(dialog.item) },
                            entryToRemove = s.list.name
                        )
                    }

                    null -> Unit
                }
            }
        }
    }
}

private enum class ListAddCategory {
    Suggested,
    Favorites
}

@Composable
private fun SuccessScreenContent(
    query: String,
    changeQuery: (String) -> Unit,
    changePagingItems: () -> Unit,
    onAddClick: (ContentItem) -> Unit,
    onClick: (ContentItem) -> Unit,
    onLongClick: (ContentItem) -> Unit,
    contentPagingItems: LazyPagingItems<StateFlow<ContentItem>>,
    snackbarHostState: SnackbarHostState,
    state: ListAddState.Success
) {
    val pagerState = rememberPagerState {
        ListAddCategory.entries.size
    }
    var searchActive by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_to_list),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    val navigator = LocalNavigator.current
                    IconButton(
                        onClick = {
                            if (searchActive)
                                searchActive = false
                            else
                                navigator?.pop()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.cancel)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding()
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            SearchBar(
                modifier = Modifier.padding(
                    horizontal = animateDpAsState(
                        if (searchActive) 0.dp else 12.dp,
                        label = "search-padding"
                    ).value
                ),
                expanded = false,
                onExpandedChange = { },
                windowInsets = WindowInsets(12.dp),
                inputField = {
                    SearchBarDefaults.InputField(query, changeQuery, changeQuery, searchActive,
                        onExpandedChange = { searchActive = it },
                        placeholder = {
                            Text(stringResource(id = R.string.search))
                        },
                        leadingIcon = {
                            IconButton(onClick = changePagingItems) {
                                Icon(
                                    imageVector = if (state.movies) Icons.Filled.Movie else Icons.Filled.Tv,
                                    contentDescription = null
                                )
                            }
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimatedVisibility(visible = query.isNotEmpty()) {
                                    IconButton(onClick = { changeQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Filled.Clear,
                                            contentDescription = null
                                        )
                                    }
                                }
                                IconButton(onClick = { changeQuery(query) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = stringResource(id = R.string.search)
                                    )
                                }
                            }
                        }
                    )
                },
            ) {
                PagingListing(
                    content = contentPagingItems,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onAddClick = onAddClick,
                    isMovie = state.movies
                )
            }
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing = 12.dp,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (ListAddCategory.entries[it]) {
                    ListAddCategory.Suggested -> {
                        CategoryListing(
                            title = "Suggested",
                            description = "from the current list items",
                            content = state.recommendations,
                            onClick = onClick,
                            onLongClick = onLongClick,
                            onAddClick = onAddClick
                        )
                    }

                    ListAddCategory.Favorites -> {
                        CategoryListing(
                            title = "Favorites",
                            showFavorite = false,
                            description = "from your favorite items",
                            content = state.favorites,
                            onClick = onClick,
                            onLongClick = onLongClick,
                            onAddClick = onAddClick
                        )
                    }
                }
            }
            PagingIndicator(
                pagerState = pagerState,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun PagingIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pagerState.pageCount) { iteration ->
            val pageOffset = (
                    (pagerState.currentPage - iteration) + pagerState
                        .currentPageOffsetFraction
                    ).absoluteValue
            val color = Color.White
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(
                        color.copy(
                            alpha = lerp(
                                start = 0.3f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            )
                        )
                    )
                    .size(10.dp)
                    .graphicsLayer {
                        val scale = lerp(
                            start = 0.7f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                        scaleY = scale
                        scaleX = scale
                    }
            )
        }
    }
}

@Composable
private fun PagingListing(
    content: LazyPagingItems<StateFlow<ContentItem>>,
    isMovie: Boolean,
    onClick: (ContentItem) -> Unit,
    onLongClick: (ContentItem) -> Unit,
    onAddClick: (ContentItem) -> Unit,
) {
    if (content.itemCount == 0) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .imePadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Find content to add to your list",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp, top = 12.dp)
                )
                Text(
                    text = "Search for ${if (isMovie) "Movies" else "TV-Shows"}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(start = 12.dp, bottom = 22.dp, top = 2.dp)
                        .graphicsLayer { alpha = 0.78f }
                )
            }
        }
    } else {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                .imePadding()
        ) {
            items(
                content.itemCount,
                content.itemKey { it.value.contentId }
            ) {
                val item by content[it]?.collectAsStateWithLifecycle() ?: return@items
                Box(Modifier.animateItem()) {
                    ContentListItem(
                        title = item.title,
                        favorite = item.favorite,
                        poster = remember(item) { item.toPoster() },
                        onClick = { onClick(item) },
                        onLongClick = { onLongClick(item) },
                    ) {
                        IconButton(
                            onClick = { onAddClick(item) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddCircleOutline,
                                contentDescription = stringResource(id = R.string.add),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            if (content.loadState.append is LoadState.Loading) {
                item(
                    key = "loading-append",
                ) {
                    PageLoadingIndicator()
                }
            }
        }
    }
}

@Composable
private fun CategoryListing(
    title: String,
    description: String,
    showFavorite: Boolean = true,
    content: List<ContentItem>,
    onClick: (ContentItem) -> Unit,
    onLongClick: (ContentItem) -> Unit,
    onAddClick: (ContentItem) -> Unit
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
    ) {
        item("category-title") {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp, top = 12.dp)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(start = 12.dp, bottom = 22.dp, top = 2.dp)
                        .graphicsLayer { alpha = 0.78f }
                )
            }
        }
        items(content, { it.itemKey }) {
            Box(Modifier.animateItem()) {
                ContentListItem(
                    title = it.title,
                    favorite = showFavorite && it.favorite,
                    poster = remember(it) { it.toPoster() },
                    onClick = { onClick(it) },
                    onLongClick = { onLongClick(it) },
                ) {
                    IconButton(
                        onClick = { onAddClick(it) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCircleOutline,
                            contentDescription = stringResource(id = R.string.add),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
package io.silv.movie.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExploreOff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.core_ui.components.Action
import io.silv.core_ui.components.EmptyScreen
import io.silv.core_ui.components.PullRefresh
import io.silv.core_ui.components.TooltipIconButton
import io.silv.core_ui.components.shimmer.ButtonPlaceholder
import io.silv.core_ui.components.shimmer.ListItemPlaceHolder
import io.silv.core_ui.components.shimmer.ShimmerHost
import io.silv.core_ui.theme.SeededMaterialTheme
import io.silv.core_ui.util.colorClickable
import io.silv.core_ui.util.rememberDominantColor
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
import io.silv.movie.R
import io.silv.movie.data.model.ContentItem
import io.silv.movie.prefrences.PosterDisplayMode
import io.silv.movie.data.supabase.model.User
import io.silv.movie.isDarkTheme
import cafe.adriel.voyager.koin.koinScreenModel
import io.silv.movie.presentation.CollectEventsWithLifecycle
import io.silv.movie.presentation.LocalContentInteractor
import io.silv.movie.presentation.LocalListInteractor
import io.silv.movie.presentation.LocalUser
import io.silv.movie.presentation.components.content.ContentListPoster
import io.silv.movie.presentation.components.content.ContentListPosterGrid
import io.silv.movie.presentation.components.content.ContentListPosterList
import io.silv.movie.presentation.components.content.rememberListUri
import io.silv.movie.presentation.components.dialog.ContentOptionsBottomSheet
import io.silv.movie.presentation.components.dialog.ListOptionsBottomSheet
import io.silv.movie.presentation.components.dialog.RemoveEntryDialog
import io.silv.movie.presentation.components.dialog.SortOptionsBottomSheet
import io.silv.movie.presentation.components.list.SpotifyTopBarLayout
import io.silv.movie.presentation.components.list.TitleWithProfilePicture
import io.silv.movie.presentation.components.list.TopBarState
import io.silv.movie.presentation.components.list.rememberTopBarState
import io.silv.movie.presentation.components.profile.UserProfileImage
import io.silv.movie.presentation.covers.EditCoverAction
import io.silv.movie.presentation.covers.ListViewCoverDialog
import io.silv.movie.presentation.covers.screenmodel.ListCoverScreenModel
import io.silv.movie.presentation.screenmodel.ListViewEvent
import io.silv.movie.presentation.screenmodel.ListViewScreenModel
import io.silv.movie.presentation.screenmodel.ListViewState
import io.silv.movie.presentation.tabs.SharedElement
import io.silv.movie.presentation.tabs.registerSharedElement
import io.silv.movie.presentation.toPoster
import org.koin.core.parameter.parametersOf

data class ListViewScreen(
    private val listId: Long = -1L,
    private val supabaseId: String = ""
): Screen {

    override val key: ScreenKey
        get() = "$listId$supabaseId"


    @Composable
    override fun Content() {

        val screenModel = koinScreenModel<ListViewScreenModel> { parametersOf(listId, supabaseId) }
        val state by screenModel.state.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.currentOrThrow
        val refreshingList by screenModel.refreshingList.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val snackBarState = remember { SnackbarHostState() }
        val listInteractor = LocalListInteractor.current
        val contentInteractor = LocalContentInteractor.current

        suspend fun showSnackBar(message: String): SnackbarResult {
            return snackBarState.showSnackbar(message)
        }

        CollectEventsWithLifecycle(screenModel) { event ->
            when (event) {
                ListViewEvent.ListDeleted -> navigator.pop()
                ListViewEvent.FailedToRemoveListFromNetwork -> showSnackBar(context.getString(R.string.failed_to_delete_list))
            }
        }

        when (val s = state) {
            is ListViewState.Error -> {
                EmptyScreen(
                    icon = Icons.Filled.ExploreOff,
                    iconSize = 182.dp,
                    actions = listOf(
                        Action(
                            R.string.retry,
                            onClick = screenModel::initializeList
                        )
                    )
                )
            }
            ListViewState.Loading -> ListLoadingScreen(Modifier.fillMaxSize())
            is ListViewState.Success -> {

                val onDismissRequest = remember { { screenModel.changeDialog(null) } }
                val listEditScreen = remember(s.list.name) { ListEditScreen(s.list.name) }

                val screenResultLauncher = rememberScreenWithResultLauncher(
                    screen = listEditScreen
                ) { result ->
                    listInteractor.editList(s.list) { it.copy(name = result.name) }
                }

                val descriptionEditScreen =
                    remember(s.list.description) { ListEditDescriptionScreen(s.list.description) }

                val descriptionResultLauncher = rememberScreenWithResultLauncher(
                    screen = descriptionEditScreen
                ) { result ->
                    listInteractor.editList(s.list) { it.copy(description = result.description) }
                }

                val changeDialog = remember {
                    { sheet: ListViewScreenModel.Dialog? -> screenModel.changeDialog(sheet) }
                }
                val toggleItemFavorite = remember {
                    { contentItem: ContentItem -> contentInteractor.toggleFavorite(contentItem) }
                }

                val listUri = rememberListUri(list = s.list)

                val primary by rememberDominantColor(
                    fallback = Color.Transparent,
                    data = when {
                        listUri != null -> listUri.uri
                        s.allItems.isEmpty() -> null
                        else -> s.allItems.first().toPoster()
                    }
                )

                SeededMaterialTheme(
                    fallback = MaterialTheme.colorScheme,
                    seedColor = primary.takeIf { it != Color.Transparent },
                    darkTheme = isDarkTheme()
                ) {
                    val user = LocalUser.current
                    val isOwnerMe by remember(user, s.list.createdBy) {
                        derivedStateOf { s.list.createdBy == user?.userId || s.list.createdBy == null }
                    }

                    SuccessScreenContent(
                        query = screenModel.query,
                        onBackPressed = {navigator.pop()},
                        refreshingList = refreshingList,
                        refreshList = screenModel::refreshList,
                        updateQuery = screenModel::updateQuery,
                        onListOptionClick = { changeDialog(ListViewScreenModel.Dialog.ListOptions) },
                        updateListViewDisplayMode = screenModel::updateListViewDisplayMode,
                        listViewDisplayMode = { screenModel.listViewDisplayMode },
                        onClick = { item ->
                            if (item.isMovie)
                                navigator.push(MovieViewScreen(item.contentId))
                            else
                                navigator.push(TVViewScreen(item.contentId))
                        },
                        onLongClick = { item ->
                            if (item.favorite) {
                                changeDialog(ListViewScreenModel.Dialog.RemoveFromFavorites(item))
                            } else {
                                toggleItemFavorite(item)
                            }
                        },
                        onOptionsClick = { changeDialog(ListViewScreenModel.Dialog.ContentOptions(it)) },
                        updateDialog = { changeDialog(it) },
                        refreshRecommendations = screenModel::refreshRecommendations,
                        onAddRecommendation = {
                            contentInteractor.addToList(s.list, it)
                        },
                        onRecommendationLongClick = { item ->
                            if (item.favorite) {
                                changeDialog(ListViewScreenModel.Dialog.RemoveFromFavorites(item))
                            } else {
                                toggleItemFavorite(item)
                            }
                        },
                        onRecommendationClick = { item ->
                            if (item.isMovie)
                                navigator.push(MovieViewScreen(item.contentId))
                            else
                                navigator.push(TVViewScreen(item.contentId))
                        },
                        startAddingClick = {
                            navigator.push(
                                ListAddScreen(s.list.id)
                            )
                        },
                        onPosterClick = { changeDialog(ListViewScreenModel.Dialog.FullCover) },
                        snackbarHostState = snackBarState,
                        isOwnerMe = isOwnerMe,
                        onUserClick = {
                            navigator.push(ProfileViewScreen(it))
                        },
                        state = s
                    )
                    when (val dialog = s.dialog) {
                        null -> Unit
                        is ListViewScreenModel.Dialog.DeleteList -> {
                            RemoveEntryDialog(
                                onDismissRequest = onDismissRequest,
                                onConfirm = screenModel::deleteList,
                                entryToRemove = s.list.name
                            )
                        }

                        is ListViewScreenModel.Dialog.ContentOptions -> {
                            val addToAnotherListScreen = remember(dialog.item) {
                                AddToListScreen(dialog.item.contentId, dialog.item.isMovie)
                            }

                            val launcher = rememberScreenWithResultLauncher(
                                screen = addToAnotherListScreen
                            ) { result ->
                                contentInteractor.addToAnotherList(result.listId, dialog.item)
                            }

                            ContentOptionsBottomSheet(
                                onDismissRequest = onDismissRequest,
                                onAddToAnotherListClick = {
                                    launcher.launch()
                                    onDismissRequest()
                                },
                                onToggleFavoriteClicked = {
                                    toggleItemFavorite(dialog.item)
                                },
                                onRemoveFromListClicked = {
                                    contentInteractor.removeFromList(s.list, dialog.item)
                                },
                                isOwnerMe = isOwnerMe,
                                item = dialog.item
                            )
                        }

                        ListViewScreenModel.Dialog.ListOptions -> {
                            ListOptionsBottomSheet(
                                onDismissRequest = onDismissRequest,
                                onAddClick = { navigator.push(ListAddScreen(s.list.id)) },
                                onEditClick = {
                                    screenResultLauncher.launch()
                                    onDismissRequest()
                                },
                                onDeleteClick = { changeDialog(ListViewScreenModel.Dialog.DeleteList) },
                                onShareClick = {
                                    listInteractor.toggleListVisibility(s.list)
                                    onDismissRequest()
                                },
                                list = s.list,
                                onChangeDescription = { descriptionResultLauncher.launch() },
                                onCopyClick = { listInteractor.copyList(s.list) },
                                isUserMe = isOwnerMe,
                                content = s.allItems,
                                onSubscribeClicked = { listInteractor.subscribeToList(s.list) },
                                onUnsubscribeClicked = { listInteractor.unsubscribeFromList(s.list) },
                                onTogglePinned = { listInteractor.togglePinned(s.list) }
                            )
                        }

                        is ListViewScreenModel.Dialog.RemoveFromFavorites -> {
                            RemoveEntryDialog(
                                onDismissRequest = onDismissRequest,
                                onConfirm = { toggleItemFavorite(dialog.item) },
                                entryToRemove = dialog.item.title
                            )
                        }

                        ListViewScreenModel.Dialog.FullCover -> {
                            val sm =
                                koinScreenModel<ListCoverScreenModel> { parametersOf(s.list.id) }
                            val list by sm.state.collectAsStateWithLifecycle()

                            LaunchedEffect(s.list.id) {
                                sm.refresh(s.list.id)
                            }

                            if (list != null) {
                                val getContent = rememberLauncherForActivityResult(
                                    ActivityResultContracts.GetContent(),
                                ) {
                                    if (it == null) return@rememberLauncherForActivityResult
                                    sm.editCover(context, it)
                                }
                                ListViewCoverDialog(
                                    items = s.allItems,
                                    list = list!!,
                                    isCustomCover = remember(list) { sm.hasCustomCover(list!!) },
                                    onShareClick = { sm.shareCover(context) },
                                    onSaveClick = { sm.saveCover(context) },
                                    snackbarHostState = sm.snackbarHostState,
                                    onEditClick = {
                                        when (it) {
                                            EditCoverAction.EDIT -> getContent.launch("image/*")
                                            EditCoverAction.DELETE -> sm.deleteCustomCover(context)
                                        }
                                    },
                                    onDismissRequest = onDismissRequest,
                                )
                            }
                        }
                        ListViewScreenModel.Dialog.SortOptions -> {
                            SortOptionsBottomSheet(
                                onDismissRequest = onDismissRequest,
                                selected = s.sortMode,
                                onSortChange = screenModel::updateSortMode,
                                list = s.list,
                                content = s.allItems
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ListLoadingScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier) { paddingValues ->
        ShimmerHost(
            Modifier.padding(paddingValues)
        ) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(
                        modifier = Modifier
                            .size(244.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                }

                Spacer(Modifier.padding(8.dp))


                Row {
                    ButtonPlaceholder(
                        Modifier.weight(0.8f)
                    )
                    Spacer(Modifier.padding(8.dp))
                    ButtonPlaceholder(
                        Modifier
                            .weight(0.2f)
                            .clip(CircleShape)
                    )
                }
            }

            repeat(6) {
                ListItemPlaceHolder()
            }
        }
    }
}

@Composable
private fun SuccessScreenContent(
    updateQuery: (String) -> Unit,
    onListOptionClick:() -> Unit,
    listViewDisplayMode: () -> PosterDisplayMode,
    updateListViewDisplayMode: (PosterDisplayMode) -> Unit,
    query: String,
    onLongClick: (item: ContentItem) -> Unit,
    onClick: (item: ContentItem) -> Unit,
    onOptionsClick: (item: ContentItem) -> Unit,
    updateDialog: (ListViewScreenModel.Dialog?) -> Unit,
    onRecommendationClick: (item: ContentItem) -> Unit,
    onRecommendationLongClick: (item: ContentItem) -> Unit,
    onAddRecommendation: (item: ContentItem) -> Unit,
    refreshRecommendations: () -> Unit,
    startAddingClick: () -> Unit,
    onPosterClick: () -> Unit,
    refreshList: () -> Unit,
    refreshingList: Boolean,
    onUserClick: (id: String) -> Unit,
    onBackPressed: () -> Unit,
    snackbarHostState: SnackbarHostState,
    isOwnerMe: Boolean,
    state: ListViewState.Success,
) {
    PullRefresh(
        refreshing = refreshingList,
        enabled = { state.list.supabaseId != null },
        onRefresh = refreshList
    ) {
        val lazyListState = rememberLazyListState()
        val lazyGridState = rememberLazyGridState()

        val topBarState = rememberTopBarState(
            if (listViewDisplayMode() is PosterDisplayMode.List)
                lazyListState
            else
                lazyGridState
        )

        LaunchedEffect(topBarState.searching) {
            if(!topBarState.searching) {
                updateQuery("")
            }
        }

        val inOverlay by remember {
            derivedStateOf { topBarState.fraction > 0.1f && !topBarState.scrollableState.isScrollInProgress }
        }
        
        SpotifyTopBarLayout(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(topBarState.connection)
                .imePadding(),
            topBarState = topBarState,
            pinnedButton = {
                FilledIconButton(
                    onClick = {
                        onListOptionClick()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = null,
                    )
                }
            },
            info = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    val sharedModifier =  Modifier
                        .registerSharedElement(
                            SharedElement.From("${SharedElement.PREFIX_LIST_NAME}${state.list.id}"),
                            inOverlay
                        )
                    if (state.user != null) {
                        TitleWithProfilePicture(
                            user = state.user,
                            name = state.list.name,
                            description = state.list.description,
                            onUserClicked = {
                                state.list.createdBy?.let { onUserClick(it) }
                            },
                            textModifier = sharedModifier
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.large)
                                .colorClickable {
                                    state.list.createdBy?.let { onUserClick(it) }
                                }
                        ) {
                            Text(
                                text = state.list.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = sharedModifier
                            )
                        }
                    }
                    ListViewDisplayMode(
                        displayMode = listViewDisplayMode,
                        setDisplayMode = updateListViewDisplayMode,
                    )
                }
            },
            search = {
                Row(Modifier.padding(horizontal = 18.dp)) {
                    Box(
                        Modifier
                            .weight(0.8f)
                            .fillMaxHeight()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                            .colorClickable {
                                topBarState.searching = !topBarState.searching
                            }
                            .padding(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Find in list", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        Modifier
                            .weight(0.2f)
                            .fillMaxHeight()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                            .colorClickable {
                                updateDialog(ListViewScreenModel.Dialog.SortOptions)
                            }
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sort", style = MaterialTheme.typography.labelLarge)
                    }
                }
            },
            poster = {
                ContentListPoster(
                    list = state.list,
                    items = state.allItems,
                    modifier = Modifier
                        .fillMaxHeight()
                        .then(
                            if (!topBarState.searching) {
                                Modifier.clickable { onPosterClick() }
                            } else {
                                Modifier
                            }
                        )
                        .registerSharedElement(SharedElement.List(state.list.id), inOverlay)
                )
            },
            topAppBar = {
                PinnedTopBar(
                    onBackPressed = onBackPressed,
                    topBarState = topBarState,
                    onQueryChanged = updateQuery,
                    query = query,
                    user = state.user,
                    name = state.list.name
                )
            },
            snackbarHostState = snackbarHostState
        ) { paddingValues ->
            when (val mode = listViewDisplayMode()) {
                is PosterDisplayMode.Grid -> {
                    ContentListPosterGrid(
                        lazyGridState = lazyGridState,
                        mode = mode,
                        items = state.items,
                        recommendations = state.recommendations,
                        onOptionsClick = onOptionsClick,
                        onLongClick = onLongClick,
                        onClick = onClick,
                        paddingValues = paddingValues,
                        onRefreshClick = refreshRecommendations,
                        refreshingRecommendations = state.refreshingRecommendations,
                        startAddingClick = startAddingClick,
                        isOwnerMe = isOwnerMe,
                    )
                }
                PosterDisplayMode.List -> {
                    ContentListPosterList(
                        lazyListState = lazyListState,
                        items = state.items,
                        onOptionsClick = onOptionsClick,
                        onLongClick = onLongClick,
                        onClick = onClick,
                        paddingValues = paddingValues,
                        onRefreshClick = refreshRecommendations,
                        recommendations = state.recommendations,
                        refreshingRecommendations = state.refreshingRecommendations,
                        onAddRecommendation = onAddRecommendation,
                        onRecommendationClick = onRecommendationClick,
                        onRecommendationLongClick = onRecommendationLongClick,
                        startAddingClick = startAddingClick,
                        isOwnerMe = isOwnerMe,
                    )
                }
            }
        }
    }
}

@Composable
fun ListViewDisplayMode(
    displayMode: () -> PosterDisplayMode,
    setDisplayMode: (PosterDisplayMode) -> Unit
) {
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
            imageVector = when (displayMode()) {
                PosterDisplayMode.List -> Icons.AutoMirrored.Filled.List
                else -> Icons.Filled.GridView
            },
            contentDescription = stringResource(id = R.string.display_mode),
            tooltip = stringResource(id = R.string.display_mode)
        )
    }
}


@Composable
fun PinnedTopBar(
    onBackPressed: () -> Unit,
    topBarState: TopBarState,
    query: String,
    onQueryChanged: (String) -> Unit,
    user: User?,
    name: String,
) {
    val fractionLerp by rememberUpdatedState(
        lerp(
            0f,
            1f,
            FastOutLinearInEasing.transform((topBarState.fraction / 0.2f).coerceIn(0f..1f))
        )
    )
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = {
                    if (topBarState.searching) {
                        topBarState.searching = false
                    } else {
                        onBackPressed()
                    }
                }
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, null)
            }
        },
        actions = {},
        title = {
            val focusRequester = remember { FocusRequester() }
            if (topBarState.searching) {

                LaunchedEffect(focusRequester) {
                    focusRequester.requestFocus()
                }

                TextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    singleLine = true,
                    placeholder = { Text(stringResource(id = R.string.search_placeholder, name)) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedVisibility(visible = query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = null
                                    )
                                }
                            }
                            IconButton(onClick = { onQueryChanged(query) }) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = stringResource(id = R.string.search)
                                )
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .focusable()
                        .focusRequester(focusRequester),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onQueryChanged(query)
                            focusRequester.freeFocus()
                        }
                    )
                )
                return@TopAppBar
            }
            if (user != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer { alpha = 1f - fractionLerp }
                ) {
                    UserProfileImage(
                        user = user,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .aspectRatio(1f),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        name,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                Text(name,  modifier = Modifier.graphicsLayer { alpha = 1f - fractionLerp })
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            MaterialTheme.colorScheme.primaryContainer.copy(
                alpha = 1f - fractionLerp
            )
        )
    )
}

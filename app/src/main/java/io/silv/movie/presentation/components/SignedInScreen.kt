package io.silv.movie.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.core_ui.components.ItemCover
import io.silv.core_ui.components.lazy.VerticalFastScroller
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
import io.silv.movie.R
import io.silv.movie.data.model.ContentItem
import io.silv.movie.data.model.ContentList
import io.silv.movie.data.supabase.model.User
import io.silv.movie.presentation.LocalListInteractor
import io.silv.movie.presentation.LocalUser
import io.silv.movie.presentation.components.content.ContentListPoster
import io.silv.movie.presentation.components.content.ContentListPreview
import io.silv.movie.presentation.components.content.ContentPreviewDefaults
import io.silv.movie.presentation.components.dialog.ListOptionsBottomSheet
import io.silv.movie.presentation.components.profile.ProfileState
import io.silv.movie.presentation.components.profile.ProfileTopBar
import io.silv.movie.presentation.screen.FavoritesViewScreen
import io.silv.movie.presentation.screen.ListAddScreen
import io.silv.movie.presentation.screen.ListEditDescriptionScreen
import io.silv.movie.presentation.screen.ListEditScreen
import io.silv.movie.presentation.screen.TitleWithAction

@Composable
fun SignedInScreen(
    snackbarHostState: SnackbarHostState,
    showOptionsClick: () -> Unit,
    subscribed: List<Pair<ContentList, List<ContentItem>>>,
    public: List<Pair<ContentList, List<ContentItem>>>,
    onProfileImageClicked: () -> Unit,
    onListClick: (ContentList) -> Unit,
    state: ProfileState.LoggedIn
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val user = LocalUser.current
    val navigator = LocalNavigator.currentOrThrow
    val listInteractor = LocalListInteractor.current

    var selectedList by remember {
        mutableStateOf<Pair<ContentList, List<ContentItem>>?>(null)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ProfileTopBar(
                scrollBehavior = scrollBehavior,
                user = user,
                onProfileImageClicked = onProfileImageClicked,
                showOptionsClicked = showOptionsClick
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        SubscribedListsView(
            paddingValues = paddingValues,
            user = user,
            subscribed = subscribed,
            public = public,
            onListClick = onListClick,
            onListLongClick = { selectedList = it },
            onFavoritesClicked = { navigator.push(FavoritesViewScreen) }
        )
    }

    selectedList?.let { (list, items) ->
        val listEditScreen = remember(list.name) { ListEditScreen(list.name) }

        val editResultLauncher = rememberScreenWithResultLauncher(
            screen = listEditScreen
        ) { result ->
            listInteractor.editList(list) { it.copy(name = result.name) }
        }

        val descriptionEditScreen =
            remember(list.description) { ListEditDescriptionScreen(list.description) }

        val descriptionResultLauncher = rememberScreenWithResultLauncher(
            screen = descriptionEditScreen
        ) { result ->
            listInteractor.editList(list) { it.copy(description = result.description) }
        }

        ListOptionsBottomSheet(
            onDismissRequest = { selectedList = null },
            onAddClick = { navigator.push(ListAddScreen(list.id)) },
            onEditClick = {
                editResultLauncher.launch()
                selectedList = null
            },
            onDeleteClick = { listInteractor.deleteList(list) },
            onShareClick = {
                listInteractor.toggleListVisibility(list)
                selectedList = null
            },
            list = list,
            onChangeDescription = { descriptionResultLauncher.launch() },
            onCopyClick = {
                listInteractor.copyList(list)
                selectedList = null
            },
            isUserMe = list.createdBy == user?.userId || list.createdBy == null,
            content = items ,
            onSubscribeClicked = {
                listInteractor.subscribeToList(list)
                selectedList = null
            },
            onUnsubscribeClicked = {
                listInteractor.unsubscribeFromList(list)
                selectedList = null
            },
            onTogglePinned = {
                listInteractor.togglePinned(list)
                selectedList = null
            }
        )
    }
}

@Composable
fun SubscribedListsView(
    paddingValues: PaddingValues,
    user: User?,
    subscribed: List<Pair<ContentList, List<ContentItem>>>,
    public: List<Pair<ContentList, List<ContentItem>>>,
    onListLongClick: (Pair<ContentList, List<ContentItem>>) -> Unit,
    onListClick: (contentList: ContentList) -> Unit,
    onFavoritesClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topPadding = paddingValues.calculateTopPadding()
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current

    VerticalFastScroller(
        listState = listState,
        topContentPadding = topPadding,
        endContentPadding = paddingValues.calculateEndPadding(layoutDirection),
        bottomContentPadding = paddingValues.calculateBottomPadding(),
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize(),
            state = listState,
            contentPadding = paddingValues,
        ) {
            if (subscribed.isNotEmpty()) {
                item("subscribed_title") {
                    TitleWithAction(
                        title = stringResource(R.string.subscribed),
                        actionLabel = "",
                        onAction = null,
                    )
                }
                subscribed.fastForEach { (list, items) ->
                    item(
                        key = "sub_${list.id}"
                    ) {
                        ContentListPreview(
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = { onListLongClick(list to items) },
                                    onClick = { onListClick(list) }
                                )
                                .animateItem()
                                .padding(8.dp),
                            cover = {
                                ContentListPoster(
                                    list = list,
                                    items = items,
                                    modifier = Modifier
                                        .aspectRatio(ItemCover.Square.ratio)
                                        .fillMaxSize()
                                        .clickable { onListClick(list) }
                                )
                            },
                            name = list.name,
                            description = list.description.ifEmpty {
                                when {
                                    items.isEmpty() -> stringResource(id = R.string.content_preview_no_items)
                                    else -> stringResource(R.string.content_preview_items, items.size)
                                }
                            }
                        )
                    }
                }
            }
            if (public.isNotEmpty() || user?.favoritesPublic == true) {
                item("public_title") {
                    TitleWithAction(
                        title = stringResource(R.string.public_lists),
                        actionLabel = "",
                        onAction = null,
                    )
                }
                if (user?.favoritesPublic == true) {
                    item(key = "favorites-items") {
                        ContentListPreview(
                            modifier = Modifier
                                .clickable { onFavoritesClicked() }
                                .padding(8.dp),
                            cover = {
                                ContentPreviewDefaults.LibraryContentPoster(Modifier.aspectRatio(1f))
                            },
                            name = stringResource(id = R.string.library_content_name),
                            description = stringResource(R.string.favorites_top_bar_title)
                        )
                    }
                }
                public.fastForEach { (list, items) ->
                    item("pub_${list.id}") {
                        ContentListPreview(
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = { onListLongClick(list to items) },
                                    onClick = { onListClick(list) }
                                )
                                .animateItem()
                                .padding(8.dp),
                            cover = {
                                ContentListPoster(
                                    list = list,
                                    items = items,
                                    modifier = Modifier
                                        .aspectRatio(ItemCover.Square.ratio)
                                        .fillMaxSize()
                                        .clickable { onListClick(list) }
                                )
                            },
                            name = list.name,
                            description = list.description.ifEmpty {
                                when {
                                    items.isEmpty() -> stringResource(id = R.string.content_preview_no_items)
                                    else -> stringResource(R.string.content_preview_items, items.size)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

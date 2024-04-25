package io.silv.movie.presentation.profile.screen

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.lerp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.core_ui.components.ItemCover
import io.silv.core_ui.components.lazy.VerticalFastScroller
import io.silv.core_ui.components.topbar.SearchLargeTopBar
import io.silv.core_ui.components.topbar.colors2
import io.silv.core_ui.util.colorClickable
import io.silv.core_ui.util.rememberDominantColor
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
import io.silv.movie.LocalUser
import io.silv.movie.R
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.user.User
import io.silv.movie.presentation.LocalListInteractor
import io.silv.movie.presentation.browse.lists.TitleWithAction
import io.silv.movie.presentation.library.components.ContentListPoster
import io.silv.movie.presentation.library.components.ContentListPreview
import io.silv.movie.presentation.library.components.ContentPreviewDefaults
import io.silv.movie.presentation.library.components.dialog.ListOptionsBottomSheet
import io.silv.movie.presentation.library.screens.FavoritesViewScreen
import io.silv.movie.presentation.library.screens.ListAddScreen
import io.silv.movie.presentation.library.screens.ListEditDescriptionScreen
import io.silv.movie.presentation.library.screens.ListEditScreen
import io.silv.movie.presentation.profile.ProfileState
import io.silv.movie.presentation.profile.UserProfileImage
import io.silv.movie.rememberProfileImageData
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SignedInScreen(
    snackbarHostState: SnackbarHostState,
    showOptionsClick: () -> Unit,
    subscribed: ImmutableList<Pair<ContentList, ImmutableList<ContentItem>>>,
    public: ImmutableList<Pair<ContentList, ImmutableList<ContentItem>>>,
    onProfileImageClicked: () -> Unit,
    onListClick: (ContentList) -> Unit,
    state: ProfileState.LoggedIn
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val user = LocalUser.current
    val navigator = LocalNavigator.currentOrThrow
    val listInteractor = LocalListInteractor.current

    val profileImageData = user.rememberProfileImageData()

    var selectedList by remember {
        mutableStateOf<Pair<ContentList, ImmutableList<ContentItem>>?>(null)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val dominantColor by rememberDominantColor(data = profileImageData)
            val background = MaterialTheme.colorScheme.background
            Column(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .drawWithCache {
                        onDrawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(dominantColor, background),
                                    endY = size.height * 0.8f
                                ),
                                alpha = 1f - scrollBehavior.state.collapsedFraction
                            )
                        }
                    }
            ) {
                SearchLargeTopBar(
                    title = { Text(user?.username.orEmpty()) },
                    actions = {
                        IconButton(onClick = showOptionsClick) {
                            Icon(imageVector = Icons.Filled.MoreVert, null)
                        }
                    },
                    navigationIcon = {
                       UserProfileImage(
                           modifier = Modifier
                               .padding(horizontal = 12.dp)
                               .size(40.dp)
                               .colorClickable {
                                   onProfileImageClicked()
                               }
                               .graphicsLayer {
                                   alpha = lerp(
                                       0f,
                                       1f,
                                       CubicBezierEasing(.8f, 0f, .8f, .15f).transform(
                                           scrollBehavior.state.collapsedFraction
                                       )
                                   )
                               },
                           contentDescription = null
                       )
                    },
                    colors = TopAppBarDefaults.colors2(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = dominantColor.copy(alpha = 0.3f)
                    ),
                    extraContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            UserProfileImage(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .colorClickable {
                                        onProfileImageClicked()
                                    },
                                contentDescription = null,
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(22.dp))
                            Column(
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = user?.username.orEmpty(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Text(
                                    text = user?.email.orEmpty(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.graphicsLayer { alpha = 0.78f }
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
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
            onDismissRequest = { selectedList = null},
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
            }
        )
    }
}

@Composable
fun SubscribedListsView(
    paddingValues: PaddingValues,
    user: User?,
    subscribed: ImmutableList<Pair<ContentList, ImmutableList<ContentItem>>>,
    public: ImmutableList<Pair<ContentList, ImmutableList<ContentItem>>>,
    onListLongClick: (Pair<ContentList, ImmutableList<ContentItem>>) -> Unit,
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
                item {
                    TitleWithAction(
                        title = stringResource(R.string.subscribed),
                        actionLabel = "",
                        onAction = null,
                    )
                }
                subscribed.fastForEach { (list, items) ->
                    item(
                        key = list.id.toString() + "subscribed"
                    ) {
                        ContentListPreview(
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = { onListLongClick(list to items) },
                                    onClick = { onListClick(list) }
                                )
                                .animateItemPlacement()
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
                item {
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
                    item(
                        key = list.id.toString() + "public"
                    ) {
                        ContentListPreview(
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = { onListLongClick(list to items) },
                                    onClick = { onListClick(list) }
                                )
                                .animateItemPlacement()
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

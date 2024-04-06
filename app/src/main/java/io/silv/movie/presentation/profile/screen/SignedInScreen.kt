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
import io.silv.core_ui.components.lazy.VerticalFastScroller
import io.silv.core_ui.components.topbar.SearchLargeTopBar
import io.silv.core_ui.components.topbar.colors2
import io.silv.core_ui.util.colorClickable
import io.silv.core_ui.util.rememberDominantColor
import io.silv.movie.LocalUser
import io.silv.movie.R
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.presentation.library.components.ContentListPosterItems
import io.silv.movie.presentation.library.components.ContentListPreview
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

    val profileImageData = user.rememberProfileImageData()

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
            subscribed = subscribed,
            public = public,
            onListClick = onListClick,
            onListLongClick = {  },
        )
    }
}

@Composable
fun SubscribedListsView(
    paddingValues: PaddingValues,
    subscribed: ImmutableList<Pair<ContentList, ImmutableList<ContentItem>>>,
    public: ImmutableList<Pair<ContentList, ImmutableList<ContentItem>>>,
    onListLongClick: (contentList: ContentList) -> Unit,
    onListClick: (contentList: ContentList) -> Unit,
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
            item { Text("Subscribed") }
            subscribed.fastForEach { (list, items) ->
                item(
                    key = list.id.toString() + "subscribed"
                ) {
                    ContentListPreview(
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = { onListLongClick(list) },
                                onClick = { onListClick(list) }
                            )
                            .animateItemPlacement()
                            .padding(8.dp),
                        cover = {
                            ContentListPosterItems(
                                list = list,
                                items = items,
                                modifier = Modifier
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
            item { Text("Public") }
            public.fastForEach { (list, items) ->
                item(
                    key = list.id.toString() + "public"
                ) {
                    ContentListPreview(
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = { onListLongClick(list) },
                                onClick = { onListClick(list) }
                            )
                            .animateItemPlacement()
                            .padding(8.dp),
                        cover = {
                            ContentListPosterItems(
                                list = list,
                                items = items,
                                modifier = Modifier
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

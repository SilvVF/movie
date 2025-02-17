package io.silv.movie.presentation.components.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInQuint
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.jeziellago.compose.markdowntext.MarkdownText
import io.silv.core_ui.components.DotSeparatorText
import io.silv.core_ui.components.NoResultsEmptyScreen
import io.silv.core_ui.components.PageLoadingIndicator
import io.silv.core_ui.components.bottomsheet.modal.ModalBottomSheet
import io.silv.core_ui.components.bottomsheet.modal.rememberModalBottomSheetState
import io.silv.core_ui.components.shimmer.ShimmerHost
import io.silv.core_ui.components.shimmer.TextPlaceholder
import io.silv.core_ui.components.topbar.runOnEnterKeyPressed
import io.silv.core_ui.util.keyboardAsState
import io.silv.movie.R
import io.silv.movie.coil.fetchers.model.UserProfileImageData
import io.silv.movie.data.supabase.model.User
import io.silv.movie.data.supabase.model.comment.PagedComment
import io.silv.movie.presentation.CollectEventsWithLifecycle
import io.silv.movie.presentation.LocalAppState
import io.silv.movie.presentation.LocalUser
import io.silv.movie.presentation.components.profile.UserProfileImage
import io.silv.movie.presentation.screen.ProfileScreen
import io.silv.movie.presentation.screen.ProfileViewScreen
import io.silv.movie.presentation.screenmodel.CommentEvent
import io.silv.movie.presentation.screenmodel.CommentsPagedType
import io.silv.movie.presentation.screenmodel.CommentsScreenModel
import io.silv.movie.presentation.screenmodel.RepliesState
import io.silv.movie.presentation.screenmodel.SendError
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

@Composable
fun CommentsBottomSheet(
    onDismissRequest: () -> Unit,
    screenModel: CommentsScreenModel,
) {
    val user = LocalUser.current
    val comments = screenModel.pagingData.collectAsLazyPagingItems()
    val state by screenModel.state.collectAsStateWithLifecycle()
    val sheetState =  rememberModalBottomSheetState()
    val navigator = LocalNavigator.currentOrThrow
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardVisible by keyboardAsState()

    BackHandler {
        if (state.replyingTo != null) {
            screenModel.updateReplyingTo(null)
        } else {
            if (sheetState.isVisible) {
                scope.launch {
                    sheetState.hide()
                    onDismissRequest()
                }
            } else {
                navigator.pop()
            }
        }
    }

    LaunchedEffect(keyboardVisible) {
        if (keyboardVisible) {
            delay(100)
            ensureActive()
            sheetState.expand()
        }
    }


    CollectEventsWithLifecycle(screenModel) {
        when (it) {
            CommentEvent.SentMessage -> lazyListState.animateScrollToItem(0)
            CommentEvent.SortChanged -> lazyListState.animateScrollToItem(0)
        }
    }

    val toolbarHeight = 48.dp
    val toolbarHeightPx = with(LocalDensity.current) { toolbarHeight.roundToPx().toFloat() }
    var toolbarOffsetHeightPx by remember { mutableFloatStateOf(toolbarHeightPx) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val old = toolbarOffsetHeightPx
                val newOffset = toolbarOffsetHeightPx + delta
                toolbarOffsetHeightPx = newOffset.coerceIn(0f, toolbarHeightPx)
                val consumed = toolbarOffsetHeightPx - old
                return available.copy(y = consumed)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                animate(
                    toolbarOffsetHeightPx,
                    if (toolbarOffsetHeightPx > toolbarHeightPx / 2) toolbarHeightPx else 0f
                ) { value, velocity ->
                    toolbarOffsetHeightPx = value
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        canDrag = false,
        modifier = Modifier
            .nestedScroll(nestedScrollConnection),
        contentWindowInsets = { WindowInsets.systemBars.only(WindowInsetsSides.Top) },
        onDismissRequest = onDismissRequest,
        dragHandle = {
            val density = LocalDensity.current
            val tooltipState = rememberTooltipState(isPersistent = true)

            Box(Modifier.wrapContentSize(), contentAlignment = Alignment.TopEnd) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BottomSheetDragHandlerNoPadding()
                    Text(
                        text = stringResource(id = R.string.comments),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(8.dp)
                    )
                    Layout(
                        {
                            Row(
                                modifier = Modifier
                                    .heightIn(toolbarHeight)
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ElevatedFilterChip(
                                    selected = screenModel.sortMode == CommentsPagedType.Newest,
                                    onClick = { screenModel.updateSortMode(CommentsPagedType.Newest) },
                                    label = { Text(stringResource(id = R.string.newest)) }
                                )
                                ElevatedFilterChip(
                                    selected = screenModel.sortMode == CommentsPagedType.Top,
                                    onClick = { screenModel.updateSortMode(CommentsPagedType.Top) },
                                    label = { Text(stringResource(id = R.string.top)) }
                                )
                            }
                        },
                        Modifier
                            .height(with(density) { toolbarOffsetHeightPx.toDp() })
                            .clipToBounds()
                            .graphicsLayer {
                                alpha = lerp(
                                    0f,
                                    1f,
                                    EaseInQuint.transform(toolbarOffsetHeightPx / toolbarHeightPx)
                                )
                            },
                    ) { measurables, constraints ->
                        val measurable = measurables[0]
                        val height = measurable.minIntrinsicHeight(constraints.maxWidth)
                        val placeable = measurable.measure(
                            constraints.copy(
                                minWidth = 0,
                                maxWidth = constraints.maxWidth,
                                minHeight = height,
                                maxHeight = height
                            )
                        )
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place((constraints.maxWidth - placeable.width) / 2, constraints.maxHeight - placeable.height)
                        }
                    }
                }

                TooltipBox(
                    state = tooltipState,
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(id = R.string.markdown_supported))
                        }
                    },
                ) {
                    IconButton(
                        onClick = { scope.launch { tooltipState.show() } },
                    ) {
                        Icon(imageVector = Icons.Filled.Info, contentDescription = null)
                    }
                }
            }
        },
        pinnedContent = {

            BackHandler(
                enabled = state.replyingTo != null,
            ) {
                screenModel.updateReplyingTo(null)
            }

            UserTextField(
                onMessageChange = screenModel::updateComment,
                onSendClick = screenModel::sendMessage,
                message = screenModel.comment,
                sending = state.sending,
                replyingTo = state.replyingTo,
                onSignInClicked = { navigator.push(ProfileScreen) },
                sendError = state.sendError.takeIf { user != null } ?: SendError.NotSignedIn
            )
        },
    ) {
        Box {
            CommentsPager(
                screenModel = screenModel,
                comments = comments,
                user = user,
                reply = screenModel::updateReplyingTo,
                onViewReplies = screenModel::fetchReplies,
                listState = lazyListState,
                replyingTo = state.replyingTo,
                onUserClicked = { navigator.push(ProfileViewScreen(it)) },
                paddingValues = PaddingValues(),
            )
            if (state.replyingTo != null) {
                ElevatedButton(
                    onClick = {
                        scope.launch {
                            for (i in 0..comments.itemCount) {
                                if (comments.peek(i) == state.replyingTo) {
                                    // -1 bc refresh states
                                    lazyListState.animateScrollToItem(i - 1)
                                    break
                                }
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ){
                    Text(text = "jump to replying comment")
                }
            }
        }
    }
}


@Composable
private fun CommentsPager(
    screenModel: CommentsScreenModel,
    listState: LazyListState,
    comments: LazyPagingItems<PagedComment>,
    user: User?,
    replyingTo: PagedComment?,
    reply: (PagedComment) -> Unit,
    onViewReplies: (PagedComment) -> Unit,
    onUserClicked: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding =  paddingValues,
    ) {
        val notLoadingAndEmpty =
            comments.loadState.append is LoadState.NotLoading
                    && comments.loadState.append is LoadState.NotLoading
                    && comments.itemCount == 0
        if (notLoadingAndEmpty) {
            item("empty_item") {
                Box(modifier = Modifier
                    .height(300.dp)
                    .fillMaxWidth(), contentAlignment = Alignment.Center) {
                    NoResultsEmptyScreen(contentPaddingValues = PaddingValues())
                }
            }
        }
        item("refreshing_items") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                when(comments.loadState.refresh) {
                    is LoadState.Error -> {
                        TextButton(onClick = { comments.retry() }) {
                            Text(text = "Retry Loading comments")
                        }
                    }
                    LoadState.Loading -> CircularProgressIndicator()
                    is LoadState.NotLoading -> Unit
                }
            }
        }
        items(
            count = comments.itemCount,
            key = comments.itemKey(),
            contentType = comments.itemContentType()
        ) {
            val comment = comments[it] ?: return@items

            val profileImageData = remember(comment.profileImage, comment.userId) {
                UserProfileImageData(
                    userId = comment.userId.orEmpty(),
                    path = comment.profileImage,
                    isUserMe = comment.userId == user?.userId,
                    fetchPath = false
                )
            }

            val background by animateColorAsState(
                targetValue = if (comment == replyingTo) {
                    MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                } else Color.Transparent
            )

            Surface(color = background) {
                CommentItem(
                    profileImageData = profileImageData,
                    comment = comment,
                    repliesState = screenModel.repliesForComment.getOrDefault(comment.id, RepliesState.Idle),
                    commentLiked = screenModel.likedComments[comment.id],
                    onReply = reply,
                    onViewReplies = onViewReplies,
                    likeComment = screenModel::likeComment,
                    unlikeComment = screenModel::unlikeComment,
                    onUserClicked = onUserClicked
                )
            }
        }
        item("loading_items") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                when(comments.loadState.append) {
                    is LoadState.Error -> {
                        TextButton(onClick = { comments.retry() }) {
                            Text(text = "Retry Loading comments")
                        }
                    }
                    LoadState.Loading -> PageLoadingIndicator()
                    is LoadState.NotLoading -> Unit
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    profileImageData: UserProfileImageData,
    comment: PagedComment,
    repliesState: RepliesState,
    commentLiked: Boolean?,
    onReply: (PagedComment) -> Unit,
    onUserClicked: (id: String) -> Unit,
    onViewReplies: (PagedComment) -> Unit,
    likeComment: (id: Long) -> Unit,
    unlikeComment: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        UserProfileImage(
            data = profileImageData,
            contentDescription = null,
            modifier = Modifier
                .size(38.dp)
                .clickable { comment.userId?.let(onUserClicked) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(Modifier.weight(1f, true)) {
            CommentContent(
                message = comment.message,
                createdAt = comment.createdAt,
                username = comment.username,
                onUserClicked = { comment.userId?.let(onUserClicked) },
                modifier = Modifier.padding(start = 8.dp)
            )
            Text(
                text = stringResource(id = R.string.reply),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .padding(vertical = 2.dp)
                    .combinedClickable(
                        onLongClick = { onViewReplies(comment) }
                    ) {
                        onReply(comment)
                    }
            )
            ReplyContent(
                repliesState = repliesState,
                onViewReplies = onViewReplies,
                comment = comment,
                onUserClicked = onUserClicked
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        LikeCount(
            comment = comment,
            liked = commentLiked,
            likeComment = likeComment,
            unlikeComment =  unlikeComment
        )
    }
}

@Composable
private fun LikeCount(
    comment: PagedComment,
    liked: Boolean?,
    likeComment: (id: Long) -> Unit,
    unlikeComment: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        val user = LocalUser.current

        val isLiked = liked ?: comment.userLiked

        val toggleLike =
            {
                when {
                    comment.userId == user?.userId -> Unit
                    !isLiked -> likeComment(comment.id)
                    else -> unlikeComment(comment.id)
                }
            }

        val likeCount by remember(isLiked, comment.likes) {
            derivedStateOf {
                val count = comment.likes + when (liked) {
                    null -> 0
                    false -> -1
                    !comment.userLiked -> 1
                    else -> 0
                } + 1
                count.toString()
            }
        }

        IconButton(
            onClick = toggleLike,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = if (isLiked || comment.userId == user?.userId)
                    Icons.Filled.Favorite
                else
                    Icons.Filled.FavoriteBorder,
                contentDescription = null
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = likeCount,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun CommentContent(
    createdAt: Instant,
    username: String?,
    message: String,
    onUserClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val appState = LocalAppState.current
            Text(
                text = username ?: "deleted user",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.clickable { onUserClicked() }
            )
            DotSeparatorText()
            Text(
                text = remember(createdAt) {
                    appState.formatDate(createdAt)
                },
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.alpha(0.78f)
            )
        }
        MarkdownText(
            isTextSelectable = true,
            markdown = message,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}



@Composable
private fun ReplyContent(
    repliesState: RepliesState,
    onViewReplies: (PagedComment) -> Unit,
    comment: PagedComment,
    onUserClicked: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = repliesState,
            label = ""
        ) { state ->
            when(state) {
                is RepliesState.Error -> Text(text = stringResource(R.string.error))
                RepliesState.Idle -> if (comment.replies == 0L) Unit else TextButton(
                    enabled = comment.replies >= 0,
                    onClick =  { onViewReplies(comment) }
                ) {
                    Text(
                        text = when (comment.replies) {
                            1L -> stringResource(id = R.string.view_reply)
                            else -> stringResource(id = R.string.view_more_replies, comment.replies)
                        },
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                RepliesState.Loading -> {
                    ShimmerHost {
                        repeat(comment.replies.toInt()) {
                            Row(Modifier.padding(8.dp)) {
                                Spacer(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurface)
                                )
                                Column(Modifier.padding(horizontal = 8.dp)) {
                                    TextPlaceholder()
                                    TextPlaceholder()
                                }
                            }
                        }
                    }
                }
                is RepliesState.Success -> {

                    var visible by rememberSaveable { mutableStateOf(true) }

                    Column {
                        AnimatedVisibility(visible = visible) {
                            Column {
                                val localUser = LocalUser.current
                                state.data.fastForEach {
                                    Row(
                                        horizontalArrangement = Arrangement.Start,
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        UserProfileImage(
                                            data = remember(it) {
                                                UserProfileImageData(
                                                    it.userId.orEmpty(),
                                                    it.userId != null && it.userId == localUser?.userId,
                                                    path = it.users?.profileImage,
                                                    fetchPath = false
                                                )
                                            },
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clickable { it.userId?.let(onUserClicked) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CommentContent(
                                            it.createdAt,
                                            it.users?.username,
                                            onUserClicked = { it.userId?.let(onUserClicked) },
                                            message = it.message,
                                        )
                                    }
                                }
                            }
                        }
                        if (!visible) {
                            TextButton(
                                onClick = { visible = !visible }
                            ) {
                                Text(
                                    text = when (state.data.size.toLong()) {
                                        0L -> ""
                                        1L -> stringResource(id = R.string.view_reply)
                                        else -> stringResource(id = R.string.view_more_replies, comment.replies)
                                    },
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        } else {
                            TextButton(
                                onClick = { visible = !visible }
                            ) {
                                Text(
                                    text = stringResource(id = R.string.hide),
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



@Preview(apiLevel = 34,
    device = "spec:parent=pixel_8"
)
@Composable
fun PreviewUserTextField() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                UserTextField(
                    onMessageChange = {},
                    onSendClick = {},
                    message = TextFieldValue("Test Messate"),
                    sendError = SendError.None,
                    sending = true
                )
                Spacer(modifier = Modifier.height(22.dp))
                UserTextField(
                    onMessageChange = {},
                    onSendClick = {},
                    message = TextFieldValue("Test Messate"),
                    sendError = SendError.NotSignedIn,
                )
                Spacer(modifier = Modifier.height(22.dp))
                UserTextField(
                    onMessageChange = {},
                    onSendClick = {},
                    message = TextFieldValue("Test Messate"),
                    sendError = SendError.Failed("Failed to send")
                )
            }
        }
    }
}

@Composable
private fun UserTextField(
    modifier: Modifier = Modifier,
    onMessageChange: (TextFieldValue) -> Unit,
    onSendClick: (String) -> Unit,
    message: TextFieldValue,
    sendError: SendError,
    replyingTo: PagedComment? = null,
    onSignInClicked: () -> Unit = {},
    sending: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var focused by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { focused }.collectLatest {
            if (it) keyboardController?.show() else keyboardController?.hide()
        }
    }

    Surface(
        modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column {
            val emojis = remember {
                listOf(
                    "\uD83E\uDD23",
                    "\uD83D\uDE43",
                    "\uD83D\uDE07",
                    "\uD83D\uDE00",
                    "\uD83E\uDD72",
                    "\uD83E\uDD13",
                    "\uD83D\uDE28",
                    "\uD83D\uDE21"
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                emojis.fastForEach { emoji ->
                    item {
                        TextButton(
                            contentPadding = PaddingValues(0.dp),
                            onClick = {
                                val msg = message.text + emoji
                                onMessageChange(message.copy(text = msg, selection = TextRange(msg.length)))
                            },
                            shape = CircleShape
                        ) {
                            Text(emoji, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Row(
                modifier = modifier
                    .padding(12.dp)
                    .systemBarsPadding(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                UserProfileImage(
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                BasicTextField(
                    value = message,
                    onValueChange = { onMessageChange(it) },
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            focused = it.isFocused
                        }
                        .runOnEnterKeyPressed(action = focusManager::clearFocus)
                        .align(Alignment.CenterVertically),
                    textStyle = MaterialTheme.typography.bodyLarge
                        .copy(color = MaterialTheme.colorScheme.onSurface),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() },
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = {

                        val textHint = @Composable {
                            if (message.text.isEmpty()) {
                                Text(
                                    text = if (replyingTo != null) {
                                        stringResource(R.string.reply_hint, replyingTo.username.orEmpty())
                                    } else stringResource(R.string.comment_hint),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }

                        when(sendError) {
                            is SendError.Failed -> {
                                Column {
                                    Text(
                                        text = stringResource(R.string.failed_to_send),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                    textHint()
                                    it()
                                }
                            }
                            SendError.None -> {
                                textHint()
                                it()
                            }
                            SendError.NotSignedIn -> {
                                TextButton(onClick = onSignInClicked) {
                                    Text(
                                        text = stringResource(id = R.string.sign_in_to_send_messages),
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    },
                )
                AnimatedContent(targetState = sending, label = "") { send ->
                    if (send) {
                        CircularProgressIndicator(
                            trackColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(4.dp)
                        )
                    } else {
                        FilledIconButton(
                            onClick = { onSendClick(message.text) },
                            enabled = message.text.isNotBlank(),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowUpward,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

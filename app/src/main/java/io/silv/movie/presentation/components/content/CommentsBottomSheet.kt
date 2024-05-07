package io.silv.movie.presentation.components.content

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.core_ui.components.DotSeparatorText
import io.silv.core_ui.components.bottomsheet.modal.ModalBottomSheet
import io.silv.core_ui.components.bottomsheet.modal.SheetValue
import io.silv.core_ui.components.bottomsheet.modal.rememberModalBottomSheetState
import io.silv.core_ui.components.shimmer.ShimmerHost
import io.silv.core_ui.components.shimmer.TextPlaceholder
import io.silv.core_ui.util.keyboardAsState
import io.silv.core_ui.voyager.ContentScreen
import io.silv.movie.R
import io.silv.movie.coil.fetchers.model.UserProfileImageData
import io.silv.movie.data.user.User
import io.silv.movie.data.user.model.comment.PagedComment
import io.silv.movie.presentation.LocalAppState
import io.silv.movie.presentation.LocalUser
import io.silv.movie.presentation.components.dialog.BottomSheetDragHandlerNoPadding
import io.silv.movie.presentation.content.screenmodel.CommentsScreenModel
import io.silv.movie.presentation.content.screenmodel.RepliesState
import io.silv.movie.presentation.profile.UserProfileImage
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

@Composable
fun ContentScreen.CommentsBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    screenModel: CommentsScreenModel,
) {
    val keyboardVisible by keyboardAsState()
    val scope = rememberCoroutineScope()
    val user = LocalUser.current
    val comments = screenModel.pagingData.collectAsLazyPagingItems()
    val state by screenModel.state.collectAsStateWithLifecycle()
    val sheetState =  rememberModalBottomSheetState()
    val listState = rememberLazyListState()

    BackHandler {
        if (sheetState.currentValue != SheetValue.Hidden) {
            scope.launch {  sheetState.hide() }
        }
    }

    LaunchedEffect(keyboardVisible) {
        if (keyboardVisible) {
            sheetState.expand()
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        canDrag = false,
        contentWindowInsets = { WindowInsets.systemBars.only(WindowInsetsSides.Top) },
        onDismissRequest = onDismissRequest,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDragHandlerNoPadding()
                Text(
                    text = stringResource(id = R.string.comments),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        pinnedContent = {
            UserTextField(
                onMessageChange = screenModel::updateComment,
                onSendClick = {},
                message = screenModel.comment,
            )
        },
    ) {
        CommentsPager(
            screenModel = screenModel,
            comments = comments,
            user = user,
            reply = {},
            onViewReplies = screenModel::fetchReplies,
            listState = listState,
            paddingValues = PaddingValues()
        )
    }
}


@Composable
private fun CommentsPager(
    screenModel: CommentsScreenModel,
    listState: LazyListState,
    comments: LazyPagingItems<PagedComment>,
    user: User?,
    reply: (PagedComment) -> Unit,
    onViewReplies: (PagedComment) -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding =  paddingValues,
    ) {
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
                    isUserMe = comment.userId == user?.userId
                )
            }

            CommentItem(
                profileImageData = profileImageData,
                comment = comment,
                repliesState = screenModel.repliesForComment.getOrDefault(comment.id, RepliesState.Idle),
                commentLiked = screenModel.likedComments[comment.id],
                onReply = reply,
                onViewReplies = onViewReplies,
                likeComment = screenModel::likeComment,
                unlikeComment = screenModel::unlikeComment
            )
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
            modifier = Modifier.size(38.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(Modifier.weight(1f, true)) {
            CommentContent(
                message = comment.message,
                createdAt = comment.createdAt,
                username = comment.username,
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
                comment = comment
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val appState = LocalAppState.current
            Text(
                text = username ?: "deleted user",
                style = MaterialTheme.typography.titleSmall
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
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}



@Composable
private fun ReplyContent(
    repliesState: RepliesState,
    onViewReplies: (PagedComment) -> Unit,
    comment: PagedComment,
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
                                                    path = it.users?.profileImage
                                                )
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CommentContent(
                                            it.createdAt,
                                            it.users?.username,
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
                                    text = when (comment.replies) {
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

@Composable
private fun UserTextField(
    modifier: Modifier = Modifier,
    onMessageChange: (String) -> Unit,
    onSendClick: (String) -> Unit,
    message: String,
) {
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
                                    onMessageChange(message + emoji)
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
                    .padding(bottom = 12.dp)
                    .padding(horizontal = 12.dp)
                    .systemBarsPadding(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                UserProfileImage(
                    contentDescription = null,
                    modifier = Modifier.size(40.0.dp)
                )
                TextField(
                    modifier = Modifier.weight(1f),
                    value = message,
                    onValueChange = onMessageChange,
                    maxLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,

                    )
                )

                FilledIconButton(
                    onClick = { onSendClick(message) },
                    enabled = message.isNotBlank()
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
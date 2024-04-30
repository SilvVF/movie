package io.silv.movie.presentation.components.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.core_ui.components.DotSeparatorText
import io.silv.core_ui.util.keyboardAsState
import io.silv.core_ui.voyager.ContentScreen
import io.silv.movie.AppState
import io.silv.movie.R
import io.silv.movie.coil.fetchers.model.UserProfileImageData
import io.silv.movie.data.user.User
import io.silv.movie.data.user.model.comment.PagedComment
import io.silv.movie.presentation.LocalAppState
import io.silv.movie.presentation.LocalUser
import io.silv.movie.presentation.content.screenmodel.CommentsPagedType
import io.silv.movie.presentation.content.screenmodel.CommentsScreenModel
import io.silv.movie.presentation.profile.UserProfileImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun ContentScreen.CommentsBottomSheet(
    onDismissRequest: () -> Unit,
    screenModel: CommentsScreenModel,
) {
    val keyboardVisible by keyboardAsState()

    var lastState by rememberSaveable {
        mutableStateOf(SheetValue.PartiallyExpanded)
    }

    val bottomSheetState=
        if (keyboardVisible) {
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
        } else {
            rememberStandardBottomSheetState(
                initialValue = lastState,
                skipHiddenState = false,
            )
        }

    LaunchedEffect(bottomSheetState) {
        snapshotFlow { bottomSheetState.currentValue }.collectLatest {
            lastState = it
        }
    }

    val scope = rememberCoroutineScope()
    val user = LocalUser.current
    val appState = LocalAppState.current
    val comments = screenModel.pagingData.collectAsLazyPagingItems()
    val state by screenModel.state.collectAsStateWithLifecycle()
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }

    val h =   with(LocalDensity.current){
        WindowInsets.systemBars.getBottom(this)
    }

    fun dismissSheet() {
        scope.launch {
            bottomSheetState.hide()
            onDismissRequest()
        }
    }

    LaunchedEffect(keyboardVisible) {
        if (!keyboardVisible && state.replyingTo != null) {
            screenModel.updateReplyingTo(null)
        }
    }

    ModalBottomSheet(
        sheetState = bottomSheetState,
        onDismissRequest = onDismissRequest,
        modifier = Modifier,
    ) {
        Surface {
            Box(
                modifier = Modifier
                    .fillMaxHeight(),
                contentAlignment = Alignment.BottomEnd
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        AnimatedVisibility(
                            state.viewingReplies != null,
                            exit = shrinkHorizontally(),
                            enter = expandHorizontally(),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    screenModel.updateViewing(null)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null
                                )
                            }
                        }
                        Text(
                            text = stringResource(
                                id = if (state.viewingReplies != null) R.string.replies_for else R.string.comments
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(
                            onClick = ::dismissSheet
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null
                            )
                        }
                    }
                    AnimatedVisibility(visible = state.viewingReplies == null) {
                        LazyRow(
                            Modifier.fillMaxWidth()
                        ) {
                            CommentsPagedType.entries.fastForEach {
                                item {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    ElevatedFilterChip(
                                        selected = screenModel.sortMode == it,
                                        onClick = { screenModel.updateSortMode(it) },
                                        label = { Text(text = it.name) }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    AnimatedContent(
                        targetState = state.viewingReplies,
                        label = "",
                        modifier = Modifier.fillMaxHeight()
                    ) { viewing ->
                        if (viewing != null) {
                            val items by screenModel.replies.collectAsStateWithLifecycle()
                            LazyColumn(
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                items(
                                    items = items
                                ) {reply ->
                                    val profileImageData = remember(reply.users?.profileImage, reply.userId) {
                                        UserProfileImageData(
                                            userId = reply.userId.orEmpty(),
                                            path = reply.users?.profileImage,
                                            isUserMe = reply.userId == user?.userId
                                        )
                                    }


                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        UserProfileImage(
                                            data = profileImageData,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Column(
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .weight(1f, true)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = reply.users?.username ?: "deleted user",
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                DotSeparatorText()
                                                Text(
                                                    text = remember(reply.createdAt) {
                                                        appState.formatDate(reply.createdAt)
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.alpha(0.78f)
                                                )
                                            }
                                            Text(
                                                text = reply.message,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            CommentsPager(
                                screenModel = screenModel,
                                comments = comments,
                                appState = appState,
                                reply = screenModel::updateReplyingTo,
                                user = user,
                                paddingValues = PaddingValues(
                                    bottom = with(LocalDensity.current) { textFieldSize.height.toDp() }
                                ),
                                onViewReplies = screenModel::updateViewing,
                                modifier = Modifier.fillMaxHeight()
                            )
                        }
                    }
                }

                CommentTextField(
                    text = screenModel.comment,
                    setText = screenModel::updateComment,
                    sending = state.sending,
                    error = state.error,
                    sendMessage = screenModel::sendMessage,
                    sendReply = screenModel::sendReply,
                    replyingTo = state.replyingTo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset {
                            IntOffset(
                                x = 0,
                                y = -bottomSheetState
                                    .requireOffset()
                                    .toInt()
                                    .coerceAtLeast(0) + h,
                            )
                        }
                        .onSizeChanged {
                            textFieldSize = it
                        }
                )
            }
        }
    }
}

@Composable
private fun CommentTextField(
    text: String,
    replyingTo: PagedComment?,
    error: Boolean,
    setText: (String) -> Unit,
    sendMessage: (String) -> Unit,
    sendReply: (String, PagedComment) -> Unit,
    sending: Boolean,
    modifier: Modifier = Modifier
) {
    val h =   with(LocalDensity.current){
        WindowInsets.systemBars.getBottom(this).toDp()
    }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(replyingTo) {
        if (replyingTo != null) {
            focusRequester.requestFocus()
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = h)
    ) {
        UserProfileImage(
            modifier = Modifier
                .padding(12.dp)
                .size(32.dp),
            contentDescription = null,
        )
        TextField(
            value = text,
            onValueChange = setText,
            isError = error,
            maxLines = 8,
            label = if (error) { { Text("Failed to send") } } else null,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            placeholder = {
                if (replyingTo == null) {
                    Text(stringResource(id = R.string.comment_hint))
                } else {
                    Text(
                        stringResource(
                            id = R.string.reply_hint,
                            replyingTo.username ?: "deleted user"
                        )
                    )
                }
            },
            modifier = Modifier
                .weight(1f)
                .focusable()
                .focusRequester(focusRequester),
        )
        Button(
            onClick = { if (replyingTo != null) sendReply(text, replyingTo) else sendMessage(text) },
            enabled = !sending && LocalUser.current != null,
            modifier = Modifier
                .padding(12.dp)
                .height(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun CommentsPager(
    screenModel: CommentsScreenModel,
    comments: LazyPagingItems<PagedComment>,
    appState: AppState,
    user: User?,
    reply: (PagedComment) -> Unit,
    onViewReplies: (PagedComment) -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
) {
    LazyColumn(
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


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                UserProfileImage(
                    data = profileImageData,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Column(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f, true)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = comment.username ?: "deleted user",
                            style = MaterialTheme.typography.titleSmall
                        )
                        DotSeparatorText()
                        Text(
                            text = remember(comment.createdAt) {
                                appState.formatDate(comment.createdAt)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.alpha(0.78f)
                        )
                    }
                    Text(
                        text = comment.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            remember(comment.replies) {
                                when(comment.replies) {
                                    0L -> "Reply"
                                    1L -> "1 Reply"
                                    else ->  "${comment.replies} Replies"
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = { reply(comment) }
                                ) {
                                    onViewReplies(comment)
                                }
                        )
                        IconButton(onClick = {
                            reply(comment)
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Message,
                                modifier = Modifier.size(16.dp),
                                contentDescription = null
                            )
                        }
                    }
                }
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    val liked by remember(screenModel.likedComments, comment.userLiked) {
                        derivedStateOf {
                            screenModel.likedComments[comment.id] ?: comment.userLiked
                        }
                    }

                    val toggleLike = remember(liked) {
                        {
                           when {
                               comment.userId == user?.userId -> Unit
                               !liked ->  screenModel.likeComment(comment.id)
                               else ->   screenModel.unlikeComment(comment.id)
                           }
                        }
                    }

                    IconButton(
                        onClick = toggleLike,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (liked || comment.userId == user?.userId)
                                Icons.Filled.Favorite
                            else
                                Icons.Filled.FavoriteBorder,
                            contentDescription = null
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = remember(liked) {
                            val likes = comment.likes + when(
                                screenModel.likedComments[comment.id]
                            ) {
                                null -> 0
                                false -> -1
                                !comment.userLiked -> 1
                                else -> 0
                            } + 1
                            likes.toString()
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
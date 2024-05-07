package io.silv.movie.presentation.components.content

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.core_ui.components.DotSeparatorText
import io.silv.core_ui.components.modal.ModalBottomSheet
import io.silv.core_ui.components.modal.SheetValue
import io.silv.core_ui.util.keyboardAsState
import io.silv.core_ui.voyager.ContentScreen
import io.silv.movie.coil.fetchers.model.UserProfileImageData
import io.silv.movie.data.prefrences.BasePreferences
import io.silv.movie.data.user.User
import io.silv.movie.data.user.model.comment.PagedComment
import io.silv.movie.presentation.LocalAppState
import io.silv.movie.presentation.LocalUser
import io.silv.movie.presentation.collectAsStateOrNull
import io.silv.movie.presentation.content.screenmodel.CommentsScreenModel
import io.silv.movie.presentation.profile.UserProfileImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
    val appState = LocalAppState.current
    val comments = screenModel.pagingData.collectAsLazyPagingItems()
    val state by screenModel.state.collectAsStateWithLifecycle()
    val sheetState =  io.silv.core_ui.components.modal.rememberModalBottomSheetState()
    val listState = rememberLazyListState()

    BackHandler {
        if (sheetState.currentValue != SheetValue.Hidden) {
            scope.launch {  sheetState.hide() }
        }
    }

    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue }
            .drop(1)
            .collectLatest {
                if (it  == SheetValue.Hidden)
                    onDismissRequest()
            }
    }


    LaunchedEffect(keyboardVisible) {
        if (keyboardVisible) {
            sheetState.expand()
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.systemBars.only(WindowInsetsSides.Top) },
        onDismissRequest = onDismissRequest,
        pinnedContent = {
            UserTextField(
                onMessageChange = screenModel::updateComment,
                onSendClick = {},
                message = screenModel.comment,
            )
        },
        //sheetShape = RoundedCornerShape(cornerRadius)
    ) {
        CommentsPager(
            screenModel = screenModel,
            comments = comments,
            user = user,
            reply = {},
            onViewReplies = {},
            listState = listState,
            paddingValues = PaddingValues(
                bottom = 0.dp
            )
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
                likedComments = screenModel.likedCommentsImmutable,
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
    likedComments: kotlinx.collections.immutable.ImmutableMap<Long, Boolean>,
    onReply: (PagedComment) -> Unit,
    onViewReplies: (PagedComment) -> Unit,
    likeComment: (id: Long) -> Unit,
    unlikeComment: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val appState = LocalAppState.current
    val user = LocalUser.current
    Row(
        modifier = modifier
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
                        when (comment.replies) {
                            0L -> "Reply"
                            1L -> "1 Reply"
                            else -> "${comment.replies} Replies"
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = { onReply(comment) }
                        ) {
                            onViewReplies(comment)
                        }
                )
                IconButton(onClick = {
                    onReply(comment)
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

            val liked by remember(likedComments, comment.userLiked) {
                derivedStateOf {
                    likedComments[comment.id] ?: comment.userLiked
                }
            }

            val toggleLike =
                {
                    when {
                        comment.userId == user?.userId -> Unit
                        !liked -> likeComment(comment.id)
                        else -> unlikeComment(comment.id)
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
                text = (comment.likes + when (
                        likedComments[comment.id]
                    ) {
                        null -> 0
                        false -> -1
                        !comment.userLiked -> 1
                        else -> 0
                    } + 1).toString(),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun UserTextField(
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

            val emojiPref = koinInject<BasePreferences>().recentEmojis()
            val recentlyUsed by  emojiPref.collectAsStateOrNull()

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                recentlyUsed.orEmpty().toList().fastForEach { emoji ->
                    TextButton(
                        onClick = {
                            onMessageChange(message + emoji)
                        }
                    ) {
                        Text(emoji)
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
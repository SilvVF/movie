package io.silv.movie.presentation.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
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
import io.silv.movie.LocalAppState
import io.silv.movie.LocalUser
import io.silv.movie.R
import io.silv.movie.UserProfileImageData
import io.silv.movie.data.user.User
import io.silv.movie.presentation.profile.UserProfileImage
import io.silv.movie.presentation.view.CommentsPagedType
import io.silv.movie.presentation.view.CommentsScreenModel
import io.silv.movie.presentation.view.PagedComment
import kotlinx.coroutines.launch

@Composable
fun ContentScreen.CommentsBottomSheet(
    onDismissRequest: () -> Unit,
    screenModel: CommentsScreenModel,
) {
    val keyboardVisible by keyboardAsState()

    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = keyboardVisible
    )

    val scope = rememberCoroutineScope()
    val user = LocalUser.current
    val appState = LocalAppState.current
    val comments = screenModel.pagingData.collectAsLazyPagingItems()
    val state by screenModel.state.collectAsStateWithLifecycle()

    fun dismissSheet() {
        scope.launch {
            bottomSheetState.hide()
            onDismissRequest()
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
                        Text(
                            text = stringResource(id = R.string.comments),
                            style = MaterialTheme.typography.titleMedium
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
                    LazyRow(
                        Modifier.fillMaxWidth()
                    ) {
                        CommentsPagedType.entries.fastForEach {
                            item {
                                Spacer(modifier = Modifier.width(4.dp))
                                ElevatedFilterChip(
                                    selected = false,
                                    onClick = { screenModel.updateSortMode(it) },
                                    label = { Text(text = it.name) }
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    CommentsPager(
                        screenModel = screenModel,
                        comments = comments,
                        appState = appState,
                        user = user,
                        modifier = Modifier
                            .fillMaxHeight()
                    )
                }

                val h =   with(LocalDensity.current){
                    WindowInsets.systemBars.getBottom(this)
                }

                CommentTextField(
                    text = screenModel.comment,
                    setText = screenModel::updateComment,
                    sending = state.sending,
                    error = state.error,
                    sendMessage = screenModel::sendMessage,
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
                )
            }
        }
    }
}

@Composable
private fun CommentTextField(
    text: String,
    error: Boolean,
    setText: (String) -> Unit,
    sendMessage: (String) -> Unit,
    sending: Boolean,
    modifier: Modifier = Modifier
) {
    val h =   with(LocalDensity.current){
        WindowInsets.systemBars.getBottom(this).toDp()
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
            label =  if (error) {
                { Text("Failed to send") }
            } else null,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            placeholder = { Text(stringResource(id = R.string.comment_hint)) },
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { sendMessage(text) },
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
    ) {
        items(
            count = comments.itemCount,
            key = comments.itemKey(),
            contentType = comments.itemContentType()
        ) {
            val comment = comments[it] ?: return@items

            val profileImageData = remember(comment.profileImage, comment.userId) {
                UserProfileImageData(
                    userId = comment.userId,
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
                            text = comment.username,
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
                    Text(
                        "Reply",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { }
                            .padding(vertical = 2.dp)
                    )
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
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = if (liked || comment.userId == user?.userId)
                                Icons.Filled.Favorite
                            else
                                Icons.Filled.FavoriteBorder,
                            contentDescription = null
                        )
                    }
                    Text(
                        text = remember(liked) {
                            val likes = comment.likes + when (
                                screenModel.likedComments[comment.id]
                            ) {
                                null -> 0
                                false -> -1
                                !comment.userLiked -> 1
                                else -> 0
                            } + 1
                            likes.toString()
                        }
                    )
                }
            }
        }
    }
}
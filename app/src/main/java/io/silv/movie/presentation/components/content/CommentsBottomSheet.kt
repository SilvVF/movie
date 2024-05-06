package io.silv.movie.presentation.components.content

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.core_ui.components.DotSeparatorText
import io.silv.core_ui.util.keyboardAsState
import io.silv.core_ui.voyager.ContentScreen
import io.silv.movie.AppState
import io.silv.movie.coil.fetchers.model.UserProfileImageData
import io.silv.movie.data.user.User
import io.silv.movie.data.user.model.comment.PagedComment
import io.silv.movie.presentation.LocalAppState
import io.silv.movie.presentation.LocalUser
import io.silv.movie.presentation.content.screenmodel.CommentsScreenModel
import io.silv.movie.presentation.profile.UserProfileImage

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

    CommentsPager(
        screenModel = screenModel,
        comments = comments,
        modifier = modifier,
        appState = appState,
        user = user,
        reply = {},
        onViewReplies = {},
        paddingValues = paddingValues
    )
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
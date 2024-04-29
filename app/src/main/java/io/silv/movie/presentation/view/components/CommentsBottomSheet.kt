package io.silv.movie.presentation.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.core_ui.components.DotSeparatorText
import io.silv.core_ui.voyager.ContentScreen
import io.silv.movie.LocalAppState
import io.silv.movie.LocalUser
import io.silv.movie.R
import io.silv.movie.UserProfileImageData
import io.silv.movie.presentation.profile.UserProfileImage
import io.silv.movie.presentation.view.CommentsPagedType
import io.silv.movie.presentation.view.CommentsScreenModel
import kotlinx.coroutines.launch

@Composable
fun ContentScreen.CommentsBottomSheet(
    onDismissRequest: () -> Unit,
    screenModel: CommentsScreenModel,
) {
    val bottomSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val user = LocalUser.current
    val appState = LocalAppState.current
    val comments = screenModel.pagingData.collectAsLazyPagingItems()

    fun dismissSheet() {
        scope.launch {
            bottomSheetState.hide()
            onDismissRequest()
        }
    }

    ModalBottomSheet(
        sheetState = bottomSheetState,
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth()
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
                LazyRow(Modifier.fillMaxWidth()) {
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
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp)
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
                                if (!liked) {
                                    screenModel.likeComment(comment.id)
                                } else {
                                    screenModel.unlikeComment(comment.id)
                                }
                            }
                        }

                        IconButton(
                            onClick = toggleLike,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = if (liked)
                                    Icons.Filled.Favorite
                                else
                                    Icons.Filled.FavoriteBorder,
                                contentDescription = null
                            )
                        }
                        Text(
                            text = remember(liked) {
                                val likes = comment.likes + when(
                                    screenModel.likedComments[comment.id]
                                ) {
                                    null -> 0
                                    false -> -1
                                    !comment.userLiked -> 1
                                    else -> 0
                                }
                                likes.toString()
                            }
                        )
                    }
                }
            }
        }
    }
}
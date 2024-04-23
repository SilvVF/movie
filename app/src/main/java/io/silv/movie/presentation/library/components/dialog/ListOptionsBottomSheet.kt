package io.silv.movie.presentation.library.components.dialog

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import io.silv.movie.R
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.presentation.library.components.BottomSheetDragHandlerNoPadding
import io.silv.movie.presentation.library.components.BottomSheetHeader
import io.silv.movie.presentation.library.components.BottomSheetItem
import io.silv.movie.presentation.library.components.ContentListPoster
import io.silv.movie.presentation.library.screenmodels.ListSortMode
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SortOptionsBottomSheet(
    onDismissRequest: () ->  Unit,
    selected: ListSortMode,
    onSortChange: (ListSortMode) -> Unit,
    list: ContentList,
    content: ImmutableList<ContentItem>
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        windowInsets = WindowInsets(0, 0, 0, 0),
        dragHandle = {
            BottomSheetDragHandlerNoPadding(Modifier.padding(top = 16.dp))
        }
    ) {
        BottomSheetHeader(
            poster = {
                ContentListPoster(
                    list = list,
                    items = content,
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                )
            },
            title = { Text(list.name) },
            description = { Text(list.description, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        )
        HorizontalDivider()

        val filters =
            remember {
                listOf(
                    Triple(R.string.title, Icons.Filled.Title, ListSortMode.Title::class),
                    Triple(R.string.recently_added, Icons.Filled.NewReleases, ListSortMode.RecentlyAdded::class),
                    Triple(R.string.movies, Icons.Filled.Movie, ListSortMode.Movie::class),
                    Triple(R.string.shows, Icons.Filled.Tv, ListSortMode.Show::class)
                )
            }

        filters.fastForEach { (resId, icon, mode) ->
            BottomSheetItem(
                title = { Text(stringResource(id = resId)) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (selected::class == mode) {
                        Icon(
                            imageVector = if (selected.ascending)
                                Icons.Filled.ArrowUpward
                            else
                                Icons.Filled.ArrowDownward,
                            contentDescription = null
                        )
                    }
                },
                onClick = {
                    val asc = if (selected::class == mode) !selected.ascending else selected.ascending
                    onSortChange(
                        when (mode) {
                            ListSortMode.Movie::class -> ListSortMode.Movie(asc)
                            ListSortMode.RecentlyAdded::class -> ListSortMode.RecentlyAdded(asc)
                            ListSortMode.Show::class -> ListSortMode.Show(asc)
                            ListSortMode.Title::class -> ListSortMode.Title(asc)
                            else -> ListSortMode.RecentlyAdded(true)
                        }
                    )
                },
                modifier = Modifier
                    .background(
                        animateColorAsState(
                            targetValue =  if (selected::class == mode) MaterialTheme.colorScheme.surface else Color.Transparent,
                            label = ""
                        ).value
                    )
            )
        }
        Spacer(
            Modifier.height(
                with(LocalDensity.current) {
                    WindowInsets.systemBars.getBottom(LocalDensity.current).toDp()
                }
            )
        )
    }
}

@Composable
fun ListOptionsBottomSheet(
    onDismissRequest: () ->  Unit,
    onAddClick: () -> Unit,
    onEditClick: () -> Unit,
    onChangeDescription: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyClick: () -> Unit,
    onSubscribeClicked: () -> Unit,
    isUserMe: Boolean,
    list: ContentList,
    content: ImmutableList<ContentItem>
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        windowInsets = WindowInsets(0, 0, 0, 0),
        dragHandle = {
            BottomSheetDragHandlerNoPadding(Modifier.padding(top = 16.dp))
        }
    ) {
        BottomSheetHeader(
            poster = {
                ContentListPoster(
                    list = list,
                    items = content,
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                )
            },
            title = { Text(list.name) },
            description = { Text(list.description, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        )
        HorizontalDivider()
        if (isUserMe) {
            BottomSheetItem(
                title = { Text(stringResource(id = R.string.options_add_to_list)) },
                icon = { Icon(imageVector = Icons.Filled.AddCircleOutline, contentDescription = null) },
                onClick = onAddClick
            )
            BottomSheetItem(
                title = { Text(stringResource(id = R.string.options_edit_list)) },
                icon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = null) },
                onClick = onEditClick
            )
            BottomSheetItem(
                title = { Text(stringResource(id = R.string.options_edit_description)) },
                icon = { Icon(imageVector = Icons.Filled.EditNote, contentDescription = null) },
                onClick = onChangeDescription
            )
            BottomSheetItem(
                title = { Text(stringResource(id = R.string.share)) },
                icon = { Icon(imageVector = Icons.Filled.Share, contentDescription = null) },
                onClick = onShareClick
            )
        }
        BottomSheetItem(
            title = { Text(stringResource(id = R.string.options_delete_list)) },
            icon = { Icon(imageVector = Icons.Filled.Close, contentDescription = null) },
            onClick = onDeleteClick
        )
        if (!isUserMe) {
            if (!list.inLibrary) {
                BottomSheetItem(
                    title = { Text(stringResource(id = R.string.subscribe)) },
                    icon = { Icon(imageVector = Icons.Filled.AddCircleOutline, contentDescription = null) },
                    onClick = onSubscribeClicked
                )
            }
            BottomSheetItem(
                title = { Text(stringResource(id = R.string.copy)) },
                icon = { Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null) },
                onClick = onCopyClick
            )
        }
        Spacer(
            Modifier.height(
                with(LocalDensity.current) {
                    WindowInsets.systemBars.getBottom(LocalDensity.current).toDp()
                }
            )
        )
    }
}
package io.silv.movie.presentation.library.components.dialog

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.silv.movie.R
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.presentation.library.components.BottomSheetDragHandlerNoPadding
import io.silv.movie.presentation.library.components.BottomSheetHeader
import io.silv.movie.presentation.library.components.BottomSheetItem
import io.silv.movie.presentation.library.components.ContentListPosterItems
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ListOptionsBottomSheet(
    onDismissRequest: () ->  Unit,
    onAddClick: () -> Unit,
    onEditClick: () -> Unit,
    onChangeDescription: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
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
                ContentListPosterItems(
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
            title = { Text(stringResource(id = R.string.options_delete_list)) },
            icon = { Icon(imageVector = Icons.Filled.Close, contentDescription = null) },
            onClick = onDeleteClick
        )
        BottomSheetItem(
            title = { Text(stringResource(id = R.string.share)) },
            icon = { Icon(imageVector = Icons.Filled.Share, contentDescription = null) },
            onClick = onShareClick
        )
        Spacer(
            Modifier.height(
                with(LocalDensity.current) {
                    WindowInsets.systemBars.getBottom(LocalDensity.current).toDp()
                }
            )
        )
    }
}
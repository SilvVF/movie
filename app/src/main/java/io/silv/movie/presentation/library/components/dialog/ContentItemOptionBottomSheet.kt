package io.silv.movie.presentation.library.components.dialog

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.silv.core_ui.components.ItemCover
import io.silv.movie.R
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.presentation.library.components.BottomSheetDragHandlerNoPadding
import io.silv.movie.presentation.library.components.BottomSheetHeader
import io.silv.movie.presentation.library.components.BottomSheetItem
import io.silv.movie.presentation.library.components.ContentPreviewDefaults
import io.silv.movie.presentation.toPoster
import kotlinx.coroutines.launch

@Composable
fun ListOptionsBottomSheet(
    onDismissRequest: () ->  Unit,
    onAddToAnotherListClick: () -> Unit,
    onToggleFavoriteClicked: () -> Unit,
    onRemoveFromListClicked: () -> Unit,
    isOwnerMe: Boolean,
    item: ContentItem,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()


    fun dismiss() {
        scope.launch {
            sheetState.hide()
            onDismissRequest()
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        windowInsets = WindowInsets(0, 0, 0, 0),
        dragHandle = {
            BottomSheetDragHandlerNoPadding(Modifier.padding(top = 16.dp))
        }
    ) {
        BottomSheetHeader(
            poster = {
                ItemCover.Square(
                    modifier =  Modifier
                        .padding(vertical = 12.dp),
                    shape = RectangleShape,
                    data = item.toPoster()
                )
            },
            title = { Text(item.title) },
            description = { Text(item.description, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        )
        HorizontalDivider()
        BottomSheetItem(
            title = {
                Text(
                    if (!item.favorite) { stringResource(id = R.string.add_to_favorites) }
                    else { stringResource(id = R.string.remove_from_favorite) }
                )
            },
            icon = {
                ContentPreviewDefaults.LibraryContentPoster(
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = {
                onToggleFavoriteClicked()
                dismiss()
            }
        )
        BottomSheetItem(
            title = { Text(stringResource(id = R.string.option_add_to_another_list)) },
            icon = { Icon(imageVector = Icons.Filled.AddCircleOutline, contentDescription = null) },
            onClick = onAddToAnotherListClick
        )
        if(isOwnerMe) {
            BottomSheetItem(
                title = { Text(stringResource(id = R.string.option_remove_from_list)) },
                icon = { Icon(imageVector = Icons.Filled.RemoveCircleOutline, contentDescription = null) },
                onClick = {
                    onRemoveFromListClicked()
                    dismiss()
                }
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
package io.silv.movie.presentation.library.view.favorite

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.silv.movie.R
import io.silv.movie.presentation.library.components.BottomSheetDragHandlerNoPadding
import io.silv.movie.presentation.library.components.BottomSheetHeader
import io.silv.movie.presentation.library.components.BottomSheetItem
import io.silv.movie.presentation.library.components.ContentPreviewDefaults

@Composable
fun FavoriteOptionsBottomSheet(
    onDismissRequest: () ->  Unit,
    onAddClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = {
            BottomSheetDragHandlerNoPadding(Modifier.padding(top = 16.dp))
        }
    ) {
        BottomSheetHeader(
            poster = {
                val posterModifier = Modifier
                    .padding(vertical = 12.dp)
               ContentPreviewDefaults.LibraryContentPoster(posterModifier)
            },
            title = { Text(stringResource(id = R.string.favorites_bottom_sheet_header)) },
            description = { Text("dkjfalksdjfklajsfklj") }
        )
        HorizontalDivider()
        BottomSheetItem(
            title = { Text(stringResource(id = R.string.options_add_to_list)) },
            icon = { Icon(imageVector = Icons.Filled.AddCircleOutline, contentDescription = null) },
            onClick = onAddClick
        )
        BottomSheetItem(
            title = { Text(stringResource(id = R.string.share)) },
            icon = { Icon(imageVector = Icons.Filled.Share, contentDescription = null) },
            onClick = onShareClick
        )
    }
}
package io.silv.movie.presentation.components.dialog

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.silv.movie.R
import io.silv.movie.presentation.components.content.ContentPreviewDefaults

@Composable
fun FavoriteOptionsBottomSheet(
    onDismissRequest: () ->  Unit,
    onAddClick: () -> Unit,
    onShareClick: () -> Unit,
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
        Spacer(
            Modifier.height(
                with(LocalDensity.current) {
                    WindowInsets.systemBars.getBottom(LocalDensity.current).toDp()
                }
            )
        )
    }
}
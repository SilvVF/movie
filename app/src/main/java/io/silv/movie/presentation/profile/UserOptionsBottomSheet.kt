package io.silv.movie.presentation.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.silv.movie.presentation.library.components.BottomSheetItem

@Composable
fun UserOptionsBottomSheet(
    deleteAccountClick: () -> Unit,
    logOutClick: () -> Unit,
    onDismiss: () -> Unit
) {
   ModalBottomSheet(
       onDismissRequest = onDismiss
   ) {
       BottomSheetItem(
           title = { Text("Log Out") },
           icon = { Icon(imageVector = Icons.AutoMirrored.Filled.Logout, null) },
           onClick = logOutClick
       )
       BottomSheetItem(
            title = { Text("Delete Account") },
            icon = { Icon(imageVector = Icons.Filled.Close, null) },
            onClick = deleteAccountClick
        )
   }
}
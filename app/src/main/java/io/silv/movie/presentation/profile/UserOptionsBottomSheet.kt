package io.silv.movie.presentation.profile

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import io.silv.movie.presentation.components.dialog.BottomSheetItem

@Composable
fun UserOptionsBottomSheet(
    deleteAccountClick: () -> Unit,
    logOutClick: () -> Unit,
    changeUsername: () -> Unit,
    onDismiss: () -> Unit
) {
   ModalBottomSheet(
       onDismissRequest = onDismiss,
       windowInsets = WindowInsets(0, 0, 0, 0)
   ) {
       BottomSheetItem(
           title = { Text("Change Username") },
           icon = { Icon(imageVector = Icons.Filled.Edit, null) },
           onClick = changeUsername
       )
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
       Spacer(
           Modifier.height(
               with(LocalDensity.current) {
                   WindowInsets.systemBars.getBottom(LocalDensity.current).toDp()
               }
           )
       )
   }
}
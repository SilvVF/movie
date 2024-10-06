package io.silv.movie.presentation.components.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import io.silv.movie.presentation.components.dialog.BottomSheetItem

@Composable
fun AccountOptionsBottomSheet(
    resetPasswordClick: () -> Unit,
    onDismiss: () -> Unit
) {
   ModalBottomSheet(
       onDismissRequest = onDismiss,
       contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
   ) {
       Surface {
           Column {
               BottomSheetItem(
                   title = { Text("Reset Password") },
                   icon = { Icon(imageVector = Icons.AutoMirrored.Filled.Logout, null) },
                   onClick = resetPasswordClick
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
   }
}
package io.silv.movie.presentation.browse.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun RemoveEntryDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    entryToRemove: String,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "cancel")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirm()
                },
            ) {
                Text(text = "remove")
            }
        },
        title = {
            Text(text = "Are you sure?")
        },
        text = {
            Text(text = "You are about to remove \"${entryToRemove}\" from your library")
        },
    )
}
package io.silv.movie.presentation.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SignedInScreen(
    snackbarHostState: SnackbarHostState,
    showOptionsClick: () -> Unit,
    state: ProfileState.LoggedIn
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.user.email.orEmpty()) },
                actions = {
                    IconButton(onClick = showOptionsClick) {
                        Icon(imageVector = Icons.Filled.MoreVert, null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            Text(state.toString())
        }
    }
}
package io.silv.movie.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@Composable
fun SignedInScreen(
    snackbarHostState: SnackbarHostState,
    showOptionsClick: () -> Unit,
    onProfileImageClicked: () -> Unit,
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
        Column(Modifier.padding(paddingValues)) {
            SubcomposeAsyncImage(
                model = state.profileImageData,
                modifier = Modifier.size(120.dp).clickable { onProfileImageClicked() },
                contentDescription = null,
                error = {
                    FilledIconButton(
                        onClick = {  },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }
            )
            Text(state.toString())
        }
    }
}
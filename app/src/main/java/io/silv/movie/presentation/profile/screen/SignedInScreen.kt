package io.silv.movie.presentation.profile.screen

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import io.silv.core_ui.components.topbar.SearchLargeTopBar
import io.silv.core_ui.components.topbar.colors2
import io.silv.core_ui.util.colorClickable
import io.silv.core_ui.util.rememberDominantColor
import io.silv.movie.R
import io.silv.movie.presentation.profile.ProfileState

@Composable
fun SignedInScreen(
    snackbarHostState: SnackbarHostState,
    showOptionsClick: () -> Unit,
    onProfileImageClicked: () -> Unit,
    state: ProfileState.LoggedIn
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val dominantColor by rememberDominantColor(data = state.profileImageData)
            val background = MaterialTheme.colorScheme.background
            Column(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .drawWithCache {
                        onDrawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(dominantColor, background),
                                    endY = size.height * 0.8f
                                ),
                                alpha = 1f - scrollBehavior.state.collapsedFraction
                            )
                        }
                    }
            ) {
                SearchLargeTopBar(
                    title = { Text(state.user?.username.orEmpty()) },
                    actions = {
                        IconButton(onClick = showOptionsClick) {
                            Icon(imageVector = Icons.Filled.MoreVert, null)
                        }
                    },
                    navigationIcon = {
                        AsyncImage(
                            model = state.profileImageData,
                            error = painterResource(id = R.drawable.user_default_proflie_icon),
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .aspectRatio(1f)
                                .colorClickable {
                                    onProfileImageClicked()
                                }
                                .graphicsLayer {
                                    alpha = lerp(
                                        0f,
                                        1f,
                                        CubicBezierEasing(.8f, 0f, .8f, .15f).transform(scrollBehavior.state.collapsedFraction)
                                    )
                                },
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    },
                    colors = TopAppBarDefaults.colors2(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = dominantColor.copy(alpha = 0.3f)
                    ),
                    extraContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            AsyncImage(
                                model = state.profileImageData,
                                error = painterResource(id = R.drawable.user_default_proflie_icon),
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .colorClickable {
                                        onProfileImageClicked()
                                    },
                                contentDescription = null,
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(22.dp))
                            Column(
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = state.user?.username.orEmpty(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Text(
                                    text = state.user?.email.orEmpty(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.graphicsLayer { alpha = 0.78f }
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Text(state.toString())
        }
    }
}
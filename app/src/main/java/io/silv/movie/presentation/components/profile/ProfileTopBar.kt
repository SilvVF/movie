package io.silv.movie.presentation.components.profile

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.silv.core_ui.components.topbar.SearchLargeTopBar
import io.silv.core_ui.components.topbar.colors2
import io.silv.core_ui.util.colorClickable
import io.silv.core_ui.util.rememberDominantColor
import io.silv.movie.data.user.User
import io.silv.movie.presentation.rememberProfileImageData

@Composable
fun ProfileTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    user: User?,
    onProfileImageClicked: () -> Unit,
    showOptionsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profileImageData = user.rememberProfileImageData()
    val dominantColor by rememberDominantColor(data = profileImageData)
    val background = MaterialTheme.colorScheme.background
    Column(
        modifier
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
            title = { Text(user?.username.orEmpty()) },
            actions = {
                IconButton(onClick = showOptionsClicked) {
                    Icon(imageVector = Icons.Filled.MoreVert, null)
                }
            },
            navigationIcon = {
                val collapsed by remember {
                    derivedStateOf { scrollBehavior.state.collapsedFraction == 1f }
                }
                UserProfileImage(
                    user = user,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .then(
                            if (collapsed) {
                                Modifier.colorClickable {
                                    onProfileImageClicked()
                                }
                            } else {
                                Modifier
                            }
                        )
                        .graphicsLayer {
                            alpha = lerp(
                                0f,
                                1f,
                                CubicBezierEasing(.8f, 0f, .8f, .15f).transform(
                                    scrollBehavior.state.collapsedFraction
                                )
                            )
                        },
                    contentDescription = null
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
                    UserProfileImage(
                        user = user,
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
                            text = user?.username.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = user?.email.orEmpty(),
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
}
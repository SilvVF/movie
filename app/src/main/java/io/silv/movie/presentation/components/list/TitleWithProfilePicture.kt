package io.silv.movie.presentation.components.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.silv.movie.R
import io.silv.movie.data.user.User
import io.silv.movie.presentation.profile.UserProfileImage


@Composable
fun TitleWithProfilePicture(
    user: User,
    name: String,
    description: String,
    textModifier: Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = textModifier.width(IntrinsicSize.Max),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserProfileImage(
                user = user,
                error = painterResource(id = R.drawable.user_default_proflie_icon),
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .aspectRatio(1f),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.graphicsLayer {
                        alpha = 0.78f
                    }
                )
            }
        }
    }
}



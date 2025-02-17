package io.silv.movie.presentation.components.content

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.movie.data.content.movie.model.Credit

fun LazyListScope.creditsPagingList(
    creditsProvider: () -> LazyPagingItems<Credit>,
    onCreditClick: (credit: Credit) -> Unit,
    onCreditLongClick: (credit: Credit) -> Unit,
    onViewClick: () -> Unit,
) {
    item(
        "credits-title"
    ) {
        Row(
            Modifier
                .padding(bottom = 8.dp)
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Series Cast",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
            )   
            TextButton(
                onClick = onViewClick
            ){
                Text(text = "View Cast")
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    item(
        key = "credits-paging-list"
    ) {
        val credits = creditsProvider()
        LazyRow(Modifier.fillMaxWidth()) {
            items(
                count = credits.itemCount,
                key = credits.itemKey { it.creditId }
            ) {
                val credit = credits[it] ?: return@items
                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .width(128.dp)
                        .aspectRatio(1f / 1.6f)
                        .shadow(3.dp, CardDefaults.elevatedShape)
                        .clip(CardDefaults.elevatedShape)
                        .combinedClickable(
                            onLongClick = { onCreditLongClick(credit) },
                            onClick =  { onCreditClick(credit) }
                        )
                ) {
                    val context = LocalContext.current
                   AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(credit.profilePath)
                            //.fallback(R.drawable.user_default_proflie_icon)
                            .crossfade(true)
                            .build(),
                       contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .aspectRatio(1f),
                        contentDescription = null
                    )
                    Column(
                        Modifier
                            .fillParentMaxHeight()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = credit.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = credit.character,
                            maxLines = 2,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.graphicsLayer { alpha = 0.78f },
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
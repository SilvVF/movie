package io.silv.movie.presentation.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Brands
import compose.icons.fontawesomeicons.brands.Youtube
import io.silv.core_ui.components.DotSeparatorText
import io.silv.core_ui.components.ItemCover
import io.silv.core_ui.components.TooltipIconButton
import io.silv.core_ui.util.playOnYoutube
import io.silv.movie.R
import io.silv.movie.data.trailers.Trailer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun VideoMediaItem(
    modifier: Modifier = Modifier,
    onThumbnailClick: () -> Unit,
    item: Trailer,
    thumbnailProvider: () -> String,
    colors: CardColors = CardDefaults.elevatedCardColors(),
) {
    ElevatedCard(
        modifier = modifier
            .padding(4.dp)
            .height(120.dp)
            .fillMaxWidth(),
        colors = colors
    ) {
        Box(Modifier.fillMaxSize()) {
            Row(
                Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                ItemCover.Rect(
                    modifier = Modifier
                        .sizeIn(maxWidth = 160.dp),
                    data = thumbnailProvider(),
                    contentDescription = stringResource(id = R.string.cover),
                    onClick = onThumbnailClick,
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    MediaItemInfo(
                        item = item,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
            if (item.official) {
                Box(modifier = Modifier.align(Alignment.BottomEnd)) {

                    val context = LocalContext.current

                    TooltipIconButton(
                        onClick = {
                            context.playOnYoutube(item.key)
                        },
                        tooltip = stringResource(id = R.string.verified),
                        imageVector = Icons.Filled.CheckCircle,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
        }
    }
}


@Composable
fun MediaItemInfo(
    item: Trailer,
    modifier: Modifier,
) {
    Column(modifier) {
        Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.alpha(0.78f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            val formattedDateTime = remember {
                val dateTime = ZonedDateTime.parse(item.publishedAt)

                val formatter =
                    DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())
                dateTime.format(formatter)
            }
            Text(
                text = item.type,
                style = MaterialTheme.typography.labelSmall
            )
            DotSeparatorText()
            Text(
                text = formattedDateTime,
                style = MaterialTheme.typography.labelSmall
            )
        }
        if (item.site == "YouTube") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.alpha(0.78f),
            ) {
                TooltipIconButton(
                    onClick = { /*TODO*/ },
                    tooltip = stringResource(id = R.string.view_on_youtube),
                    imageVector = FontAwesomeIcons.Brands.Youtube,
                    modifier = Modifier.size(22.dp)
                )
                DotSeparatorText(modifier = Modifier.align(Alignment.CenterVertically))
                Text(stringResource(id = R.string.youtube), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
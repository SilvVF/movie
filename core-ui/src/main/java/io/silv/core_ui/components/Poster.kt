package io.silv.core_ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.silv.data.Movie

fun Movie.toPoster(): Poster {
    return Poster(
        id = id,
        url = posterUrl,
        title = title,
    )
}

data class Poster(
    val id: Long,
    val url: String?,
    val title: String,
    val type: PosterType = PosterType.Default
) {

    enum class PosterType(val ratio: Float) {
        Square(1f), Default(2f / 3f)
    }

    @Composable
    fun Render() {
        Box(
            Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .height((LocalConfiguration.current.screenHeightDp / 3).dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = this@Poster,
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(type.ratio),
                placeholder = ColorPainter(Color(0xFF18161B)),
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )
            Text(
                text = title,
                maxLines = 2,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFEDE0DD),
                modifier =
                Modifier
                    .fillMaxWidth()
                    .drawWithCache {
                        onDrawBehind {
                            drawRect(
                                brush =
                                Brush.verticalGradient(
                                    colors =
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Black.copy(alpha = 0.9f),
                                    ),
                                ),
                            )
                        }
                    }
                    .padding(
                        top = 32.dp,
                        bottom = 12.dp,
                        start = 6.dp,
                        end = 6.dp,
                    )
                    .align(Alignment.BottomCenter),
            )
        }
    }
}
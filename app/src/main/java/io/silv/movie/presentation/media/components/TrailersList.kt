package io.silv.movie.presentation.media.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.silv.movie.data.content.trailers.Trailer

fun LazyListScope.trailersList(
    trailers: List<Trailer>,
    onClick: (trailer: Trailer) -> Unit,
    onYoutubeClick: (trailer: Trailer) -> Unit
) {
    item(key = "trailers-title") {
        Text(
            text = "Trailers",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp).padding(horizontal = 8.dp)
        )
    }
    items(
        items = trailers,
        key = { it.id }
    ) {
        VideoMediaItem(
            modifier = Modifier.clickable { onClick(it) },
            onThumbnailClick = { onClick(it) },
            item = it,
            thumbnailProvider = {
                if (it.site == "YouTube") {
                    "https://img.youtube.com/vi/${it.key}/0.jpg"
                } else {
                    ""
                }
            }
        )
    }
}
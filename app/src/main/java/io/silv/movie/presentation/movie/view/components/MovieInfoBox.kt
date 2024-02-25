package io.silv.movie.presentation.movie.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.silv.core.Status
import io.silv.core_ui.components.DotSeparatorText
import io.silv.core_ui.components.ItemCover
import io.silv.core_ui.components.PosterData
import io.silv.core_ui.util.clickableNoIndication
import io.silv.core_ui.util.copyToClipboard

@Composable
fun MovieInfoBox(
    isTabletUi: Boolean,
    appBarPadding: Dp,
    title: String,
    author: String?,
    artist: String?,
    sourceName: String,
    isStubSource: Boolean,
    coverDataProvider: () -> PosterData,
    status: Status?,
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {

    Box(modifier = modifier) {
        // Backdrop
        val backdropGradientColors = listOf(
            Color.Transparent,
            MaterialTheme.colorScheme.background,
        )
        AsyncImage(
            model = coverDataProvider(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(colors = backdropGradientColors),
                    )
                }
                .blur(4.dp)
                .alpha(0.2f),
        )

        // Anime & source info
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            if (!isTabletUi) {
                MovieAndSourceTitlesSmall(
                    appBarPadding = appBarPadding,
                    coverDataProvider = coverDataProvider,
                    onCoverClick = onCoverClick,
                    title = title,
                    doSearch = doSearch,
                    author = author,
                    artist = artist,
                    status = status,
                    sourceName = sourceName,
                    isStubSource = isStubSource,
                )
            } else {
                MovieAndSourceTitlesLarge(
                    appBarPadding = appBarPadding,
                    coverDataProvider = coverDataProvider,
                    onCoverClick = onCoverClick,
                    title = title,
                    doSearch = doSearch,
                    author = author,
                    artist = artist,
                    status = status,
                    sourceName = sourceName,
                    isStubSource = isStubSource,
                )
            }
        }
    }
}

@Composable
private fun MovieAndSourceTitlesLarge(
    appBarPadding: Dp,
    coverDataProvider: () -> PosterData,
    onCoverClick: () -> Unit,
    title: String,
    doSearch: (query: String, global: Boolean) -> Unit,
    author: String?,
    artist: String?,
    status: Status?,
    sourceName: String,
    isStubSource: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ItemCover.Book(
            modifier = Modifier.fillMaxWidth(0.65f),
            data = coverDataProvider(),
            contentDescription = "cover",
            onClick = onCoverClick,
        )
        Spacer(modifier = Modifier.height(16.dp))
        MovieContentInfo(
            title = title,
            doSearch = doSearch,
            author = author,
            artist = artist,
            status = status,
            sourceName = sourceName,
            isStubSource = isStubSource,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MovieAndSourceTitlesSmall(
    appBarPadding: Dp,
    coverDataProvider: () -> PosterData,
    onCoverClick: () -> Unit,
    title: String,
    doSearch: (query: String, global: Boolean) -> Unit,
    author: String?,
    artist: String?,
    status: Status?,
    sourceName: String,
    isStubSource: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemCover.Book(
            modifier = Modifier
                .sizeIn(maxWidth = 100.dp)
                .align(Alignment.Top),
            data = coverDataProvider(),
            contentDescription = "cover",
            onClick = onCoverClick,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MovieContentInfo(
                title = title,
                doSearch = doSearch,
                author = author,
                artist = artist,
                status = status,
                sourceName = sourceName,
                isStubSource = isStubSource,
            )
        }
    }
}
@Composable
private fun ColumnScope.MovieContentInfo(
    title: String,
    doSearch: (query: String, global: Boolean) -> Unit,
    author: String?,
    artist: String?,
    status: Status?,
    sourceName: String,
    isStubSource: Boolean,
    textAlign: TextAlign? = LocalTextStyle.current.textAlign,
) {
    val context = LocalContext.current
    Text(
        text = title.ifBlank { "unknown" },
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.clickableNoIndication(
            onLongClick = {
                if (title.isNotBlank()) {
                    context.copyToClipboard(
                        title,
                        title,
                    )
                }
            },
            onClick = { if (title.isNotBlank()) doSearch(title, true) },
        ),
        textAlign = textAlign,
    )

    Spacer(modifier = Modifier.height(2.dp))

    Row(
        modifier = Modifier.alpha(0.78f),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.PersonOutline,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = author?.takeIf { it.isNotBlank() }
                ?: "unknown",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .clickableNoIndication(
                    onLongClick = {
                        if (!author.isNullOrBlank()) {
                            context.copyToClipboard(
                                author,
                                author,
                            )
                        }
                    },
                    onClick = { if (!author.isNullOrBlank()) doSearch(author, true) },
                ),
            textAlign = textAlign,
        )
    }

    if (!artist.isNullOrBlank() && author != artist) {
        Row(
            modifier = Modifier.alpha(0.78f),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Brush,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .clickableNoIndication(
                        onLongClick = { context.copyToClipboard(artist, artist) },
                        onClick = { doSearch(artist, true) },
                    ),
                textAlign = textAlign,
            )
        }
    }

    Spacer(modifier = Modifier.height(2.dp))

    Row(
        modifier = Modifier.alpha(0.78f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (status) {
                Status.InProduction -> Icons.Outlined.Schedule
                Status.PostProduction -> Icons.Outlined.DoneAll
                Status.Rumored -> Icons.Outlined.AttachMoney
                Status.Released -> Icons.Outlined.Done
                Status.Canceled -> Icons.Outlined.Close
                Status.Planned -> Icons.Outlined.Pause
                else -> Icons.Outlined.Block
            },
            contentDescription = null,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(16.dp),
        )
        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
            Text(
                text = status?.toString() ?: "",
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            DotSeparatorText()
            if (isStubSource) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = sourceName,
                modifier = Modifier.clickableNoIndication {
                    doSearch(
                        sourceName,
                        false,
                    )
                },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}
package io.silv.movie.presentation.movie.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import io.silv.data.movie.model.Genre
import kotlinx.collections.immutable.ImmutableList

private val CloseDialogButtonSize = 72.dp
private val CloseDialogButtonPadding = 12.dp

@Composable
fun CategorySelectDialog(
    onDismissRequest: () -> Unit,
    genres: ImmutableList<Genre>,
    selectedGenres: ImmutableList<Genre>,
    clearAllSelected: () -> Unit,
    onGenreSelected: (Genre) -> Unit,
) {
    Popup {
        Surface(color = Color.Black.copy(alpha = 0.8f)) {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Spacer(
                            Modifier
                                .windowInsetsPadding(WindowInsets.systemBars)
                                .height(CloseDialogButtonPadding + CloseDialogButtonSize)
                        )
                    }
                    item {
                        GenreListingItem(
                            onClick = clearAllSelected,
                            genre = Genre("Home", id = null),
                            selected = selectedGenres.isEmpty()
                        )
                    }
                    items(
                        items = genres,
                        key = { it.name + it.id },
                        contentType = { Genre::class.hashCode() }
                    ) { genre ->
                        val selected = remember(genre, selectedGenres) { genre in selectedGenres }
                        GenreListingItem(
                            onClick = { onGenreSelected(genre) },
                            genre = genre,
                            selected = selected
                        )
                    }
                    item {
                        Spacer(
                            Modifier
                                .windowInsetsPadding(WindowInsets.systemBars)
                                .height(CloseDialogButtonPadding + CloseDialogButtonSize)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.3f)
                        .drawWithCache {
                            onDrawBehind {
                                drawRect(
                                    brush = Brush.verticalGradient(
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
                )
                Box(
                    modifier = Modifier
                        .padding(CloseDialogButtonPadding)
                        .align(Alignment.BottomCenter)
                        .size(CloseDialogButtonSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(
                            onClick = onDismissRequest,
                            enabled = true,
                            role = Role.Button,
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun GenreListingItem(
    onClick: () -> Unit,
    genre: Genre,
    selected: Boolean,
) {
    Text(
        text = genre.name,
        style = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (!selected) 0.78f else 1f
            ),
            fontWeight = if(!selected) FontWeight.Normal else FontWeight.SemiBold
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .clip(CircleShape)
            .clickable(
                enabled = true,
                role = Role.Button,
                onClick = onClick
            )
            .padding(6.dp)
    )
}
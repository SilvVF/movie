package io.silv.movie.presentation.library.view.list

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.silv.core_ui.components.PosterLargeTopBar
import io.silv.core_ui.components.PosterTopBarState
import io.silv.core_ui.components.colors2
import io.silv.core_ui.util.rememberDominantColor
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.library.components.ContentPreviewDefaults
import io.silv.movie.presentation.library.components.PosterLargeTopBarDefaults
import io.silv.movie.presentation.toPoster
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ListViewTopBar(
    state: PosterTopBarState,
    query: () -> String,
    changeQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    displayMode: () -> PosterDisplayMode,
    setDisplayMode: (PosterDisplayMode) -> Unit,
    onListOptionClicked: () -> Unit,
    contentListProvider: () -> ContentList,
    items: () -> ImmutableList<ContentItem>,
    modifier: Modifier = Modifier,
) {
    val content= items()
    val primary by rememberDominantColor(
        data = when  {
            content.isEmpty() -> null
            else -> content.first().toPoster()
        }
    )
    val background = MaterialTheme.colorScheme.background
    val primaryAnimated by animateColorAsState(
        targetValue = primary,
        label = ""
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
            .drawWithCache {
                onDrawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            colors = if (items().isEmpty()) {
                                listOf(primaryAnimated, background)
                            } else {
                                listOf(primaryAnimated, background)
                            },
                            endY = size.height * 0.8f
                        ),
                        alpha = if(state.isKeyboardOpen) 0f else 1f - state.progress
                    )
                }
            },
    ) {
        val contentList = contentListProvider()
        PosterLargeTopBar(
            state = state,
            title = {
                Text(text = contentList.name) },
            colors = TopAppBarDefaults.colors2(
                containerColor = Color.Transparent,
                scrolledContainerColor = primary.copy(alpha = 0.2f)
            ),
            navigationIcon = {
                PosterLargeTopBarDefaults.BackArrowIcon(
                    isKeyboardOpen = state.isKeyboardOpen
                )
            },
            actions = {
                PosterLargeTopBarDefaults.Actions(
                    displayMode = displayMode,
                    setDisplayMode = setDisplayMode,
                    onListOptionClicked = onListOptionClicked
                )
            },
            posterContent = {
                ContentPreviewDefaults.LibraryContentPoster(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxHeight()
                )
            },
        ) {
            PosterLargeTopBarDefaults.SearchInputField(
                query = query,
                onSearch = onSearch,
                changeQuery = changeQuery,
                placeholder = "Search ${contentList.name}"
            )
        }
    }
}




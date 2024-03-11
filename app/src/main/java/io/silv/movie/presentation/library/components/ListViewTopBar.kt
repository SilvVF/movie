package io.silv.movie.presentation.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.core_ui.components.PosterLargeTopBar
import io.silv.core_ui.components.SearchBarInputField
import io.silv.core_ui.components.colors2
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ListViewTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    query: () -> String,
    changeQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    contentListProvider: () -> ContentList,
    items: () -> ImmutableList<ContentItem>,
    modifier: Modifier = Modifier,
) {
    val barExpandedFully by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction == 0.0f }
    }

    val barFullyCollapsed by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction == 1f }
    }

    val colors = TopAppBarDefaults.colors2(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = if (barFullyCollapsed)
            Color.Transparent
        else {
            MaterialTheme.colorScheme.surface
        }
    )

    val appBarContainerColor by rememberUpdatedState(
        colors.containerColor(scrollBehavior.state.collapsedFraction)
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(),
    ) {
        PosterLargeTopBar(
            title = {
                val list = contentListProvider()
                Text(text = list.name)
            },
            scrollBehavior = scrollBehavior,
            colors = colors,
            navigationIcon = {
                val navigator = LocalNavigator.current
                if (navigator?.canPop == true) {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            },
            posterContent = {progress ->
                val posterModifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        alpha = 1f - progress
                    }
                val content= items()
                when  {
                    content.isEmpty() -> {
                        ContentPreviewDefaults.PlaceholderPoster(
                            modifier = posterModifier
                        )
                    }
                    content.size < 4 -> {
                        ContentPreviewDefaults.SingleItemPoster(
                            item = content.first(),
                            modifier = posterModifier
                        )
                    }
                    else -> {
                        ContentPreviewDefaults.MultiItemPoster(
                            modifier = posterModifier,
                            items = content
                        )
                    }
                }
            },
        ) { progress ->
            Column(
                Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = 1f - progress
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {

                val focusManager = LocalFocusManager.current
                val list = contentListProvider()
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.Start)
                )
                SearchBarInputField(
                    query = query(),
                    placeholder = {
                        Text( "Search list...")
                    },
                    onQueryChange = { changeQuery(it) },
                    onSearch = {
                        onSearch(it)
                        focusManager.clearFocus(false)
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedVisibility(visible = query().isNotEmpty()) {
                                IconButton(onClick = { changeQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = null
                                    )
                                }
                            }
                            IconButton(onClick = { onSearch(query()) }) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                )
            }
        }
    }
}


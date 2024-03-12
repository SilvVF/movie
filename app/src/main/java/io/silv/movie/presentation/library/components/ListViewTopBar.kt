package io.silv.movie.presentation.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.core_ui.components.PosterLargeTopBar
import io.silv.core_ui.components.SearchBarInputField
import io.silv.core_ui.components.TooltipIconButton
import io.silv.core_ui.components.colors2
import io.silv.core_ui.util.keyboardAsState
import io.silv.core_ui.util.rememberDominantColor
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.toPoster
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ListViewTopBar(
    scrollBehavior: TopAppBarScrollBehavior?,
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
    val isKeyboardOpen by keyboardAsState()
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
                        alpha = if(isKeyboardOpen) 0f else 1f - (scrollBehavior?.state?.collapsedFraction ?: 0f)
                    )
                }
            },
    ) {
        PosterLargeTopBar(
            title = {
                val list = contentListProvider()
                Text(text = list.name)
            },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.colors2(
                containerColor = Color.Transparent,
                scrolledContainerColor = primary.copy(
                    alpha = 0.2f
                )
            ),
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
            actions = {
                var dropDownVisible by remember { mutableStateOf(false) }

                Box(contentAlignment = Alignment.BottomCenter) {
                    DropdownMenu(
                        expanded = dropDownVisible,
                        onDismissRequest = { dropDownVisible = false }
                    ) {
                        PosterDisplayMode.values.forEach {
                            DropdownMenuItem(
                                trailingIcon = {
                                    RadioButton(
                                        selected = displayMode() == it,
                                        onClick = { setDisplayMode(it) }
                                    )
                                },
                                text = {
                                    Text(
                                        remember {
                                            it.toString()
                                                .split(Regex("(?<=[a-z])(?=[A-Z])"))
                                                .joinToString(" ")
                                        }
                                    )
                                },
                                onClick = { setDisplayMode(it) }
                            )
                        }
                    }
                    TooltipIconButton(
                        onClick = { dropDownVisible = true },
                        imageVector = when (displayMode()) {
                            PosterDisplayMode.List -> Icons.AutoMirrored.Filled.List
                            else -> Icons.Filled.GridView
                        },
                        contentDescription = null,
                        tooltip = "Display Mode"
                    )
                }
                IconButton(onClick = onListOptionClicked) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = null
                    )
                }
            },
            posterContent = {progress ->
                val posterModifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        alpha = 1f - progress
                    }
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
                    }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                val focusManager = LocalFocusManager.current
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




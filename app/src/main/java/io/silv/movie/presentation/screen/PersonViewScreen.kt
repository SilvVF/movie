package io.silv.movie.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.DotSeparatorText
import io.silv.core_ui.components.ItemCover
import io.silv.core_ui.components.PullRefresh
import io.silv.core_ui.components.lazy.FastScrollLazyColumn
import io.silv.core_ui.components.topbar.PosterLargeTopBar
import io.silv.core_ui.components.topbar.colors2
import io.silv.core_ui.components.topbar.rememberPosterTopBarState
import io.silv.core_ui.util.clickableNoIndication
import io.silv.core_ui.util.colorClickable
import io.silv.core_ui.util.copyToClipboard
import io.silv.core_ui.util.rememberDominantColor
import io.silv.movie.koin4ScreenModel
import io.silv.movie.presentation.screenmodel.PersonViewScreenModel
import io.silv.movie.presentation.screenmodel.PersonViewState
import io.silv.movie.presentation.components.content.movie.ExpandableSummary
import org.koin.core.parameter.parametersOf


@Composable
private fun PersonInfoItem(
    label: String,
    text: String
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .colorClickable {
                context.copyToClipboard(label, text)
            }
            .padding(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
        )
        DotSeparatorText()
        Text(text,  style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.alpha(0.78f),)
    }
}

private val whitespaceLineRegex = Regex("[\\r\\n]{2,}", setOf(RegexOption.MULTILINE))

data class PersonViewScreen(
    val personId: Long,
    val name: String,
    val posterPath: String? = null,
): Screen {

    override val key: ScreenKey
        get() = personId.toString()

    @Composable
    override fun Content() {

        val screenModel = koin4ScreenModel<PersonViewScreenModel> { parametersOf(personId, posterPath.orEmpty()) }
        val credits = screenModel.credits.collectAsLazyPagingItems()
        val state by screenModel.state.collectAsStateWithLifecycle()

        val navigator = LocalNavigator.currentOrThrow
        val hazeState = remember { HazeState() }
        val primary by rememberDominantColor(data = posterPath)
        val background = MaterialTheme.colorScheme.background
        val topBarState = rememberPosterTopBarState()

        PullRefresh(
            refreshing = screenModel.refreshing,
            enabled = { true },
            onRefresh = screenModel::refresh
        ) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize()
                        .drawWithCache {
                            onDrawBehind {
                                drawRect(
                                    Brush.verticalGradient(
                                        colors = listOf(primary, background),
                                        endY = size.height * 0.8f
                                    ),
                                    alpha = if (topBarState.isKeyboardOpen) 0f else 1f - topBarState.progress
                                )
                            }
                        }
                        .hazeChild(hazeState),
                ) {
                    PosterLargeTopBar(
                        state = topBarState,
                        maxHeight = 284.dp,
                        title = {
                            Text(
                                text = name,
                                modifier = Modifier.padding(12.dp)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null
                                )
                            }
                        },
                        colors = TopAppBarDefaults.colors2(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = primary.copy(alpha = 0.2f)
                        ),
                        posterContent = {
                            ItemCover.Square(
                                data = posterPath,
                                modifier = Modifier
                                    .fillMaxHeight()
                            )
                        }
                    )
                }
            },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets
                .exclude(WindowInsets.systemBars),
            modifier = Modifier.nestedScroll(topBarState.scrollBehavior.nestedScrollConnection)
        ) { paddingValues ->
            FastScrollLazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxSize()
                    .haze(
                        state = hazeState,
                        style = HazeDefaults
                            .style(
                                backgroundColor = MaterialTheme.colorScheme.background
                            ),
                    ),
            ) {
                item {
                    when (val s = state) {
                        PersonViewState.Error -> {}
                        PersonViewState.Loading -> {}
                        is PersonViewState.Success -> {
                            Column(
                                Modifier
                                    .padding(horizontal = 14.dp)
                            ) {
                                val (expanded, onExpanded) = rememberSaveable {
                                    mutableStateOf(false)
                                }
                                val desc = s.info.bio.takeIf { it.isNotBlank() } ?: ""
                                val trimmedDescription = remember(desc) {
                                    desc
                                        .replace(whitespaceLineRegex, "\n")
                                        .trimEnd()
                                }
                                if(s.info.placeOfBirth.isNotBlank()) {
                                   PersonInfoItem(label = "Place of birth", text = s.info.placeOfBirth)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                s.info.birthday?.let { birthday ->
                                    PersonInfoItem(label = "Birthday", text =birthday)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                s.info.deathday?.let { deathday ->
                                    PersonInfoItem(label = "Death day", text = deathday)
                                }
                                Text(
                                    text = "Biography",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                                ExpandableSummary(
                                    expandedDescription = desc,
                                    shrunkDescription = trimmedDescription,
                                    expanded = expanded,
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .padding(horizontal = 2.dp)
                                        .clickableNoIndication { onExpanded(!expanded) },
                                )
                            }
                        }
                    }
                }
                items(
                    count = credits.itemCount,
                    key = credits.itemKey { it.creditId }
                ) {
                    val credit = credits[it] ?: return@items
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .heightIn(0.dp, 72.dp)
                            .colorClickable {
                                navigator.push(
                                    if(credit.isMovie) {
                                        MovieViewScreen(credit.contentId)
                                    } else {
                                        TVViewScreen(credit.contentId)
                                    }
                                )
                            },
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.2f, fill = false)
                                .fillMaxHeight()
                        ) {
                            ItemCover.Square(credit.posterPath)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(
                            modifier = Modifier
                                .weight(0.8f, true)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = credit.title,
                                maxLines = 2,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = credit.character,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                modifier = Modifier.graphicsLayer { alpha = 0.78f }
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}
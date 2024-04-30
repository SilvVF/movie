package io.silv.movie.presentation.content.screen

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.request.ImageRequest
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.ItemCover
import io.silv.core_ui.components.lazy.FastScrollLazyColumn
import io.silv.core_ui.components.topbar.PosterLargeTopBar
import io.silv.core_ui.components.topbar.colors2
import io.silv.core_ui.components.topbar.rememberPosterTopBarState
import io.silv.core_ui.util.colorClickable
import io.silv.core_ui.util.rememberDominantColor
import io.silv.movie.R
import io.silv.movie.presentation.content.screenmodel.CreditsViewScreenModel
import org.koin.core.parameter.parametersOf


data class CreditsViewScreen(
    val contentId: Long,
    val isMovie: Boolean,
): Screen {

    override val key: ScreenKey
        get() = "$contentId$isMovie"

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<CreditsViewScreenModel> { parametersOf(contentId, isMovie) }

        val poster by screenModel.state.collectAsStateWithLifecycle()
        val credits = screenModel.credits.collectAsLazyPagingItems()
        val navigator = LocalNavigator.currentOrThrow
        val hazeState = remember { HazeState() }

        val primary by rememberDominantColor(data = poster)
        val background = MaterialTheme.colorScheme.background
        val state = rememberPosterTopBarState()

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
                                    alpha = if (state.isKeyboardOpen) 0f else 1f - state.progress
                                )
                            }
                        }
                        .hazeChild(hazeState),
                ) {
                    PosterLargeTopBar(
                        state = state,
                        maxHeight = 284.dp,
                        title = { Text(
                            text = poster?.title.orEmpty(),
                            modifier = Modifier.padding(12.dp)
                        ) },
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
                                data = poster,
                                modifier = Modifier
                                    .fillMaxHeight()
                            )
                        }
                    )
                }
            },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets
                .exclude(WindowInsets.systemBars),
            modifier = Modifier.nestedScroll(state.scrollBehavior.nestedScrollConnection)
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
                items(
                    count = credits.itemCount,
                    key = credits.itemKey { it.creditId }
                ) {
                    val credit = credits[it] ?: return@items
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .heightIn(0.dp, 72.dp)
                            .colorClickable(color = primary) {
                                credit.personId?.let {
                                    navigator.push(
                                        PersonViewScreen(credit.personId, credit.name, credit.profilePath)
                                    )
                                }
                            },
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.2f, fill = false)
                                .fillMaxHeight()
                        ) {
                            val context = LocalContext.current
                            ItemCover.Square(
                                data = ImageRequest.Builder(context)
                                    .data(credit.profilePath)
                                    .fallback(R.drawable.user_default_proflie_icon)
                                    .crossfade(true)
                                    .build(),
                            )
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
                                text = credit.name,
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
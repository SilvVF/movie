package io.silv.movie.presentation.result.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.silv.core_ui.components.PullRefresh
import io.silv.core_ui.components.topbar.TopAppBarWithBottomContent
import io.silv.core_ui.util.colorClickable
import io.silv.core_ui.voyager.ScreenResult
import io.silv.core_ui.voyager.ScreenWithResult
import io.silv.core_ui.voyager.setScreenResult
import io.silv.movie.R
import io.silv.movie.presentation.PlaceHolderColors
import io.silv.movie.presentation.result.screenmodel.SelectProfileImageScreenModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize


object SelectProfileImageScreen: ScreenWithResult<SelectProfileImageScreen.ImageResult> {

    @Parcelize
    data class ImageResult(
        val path: String
    ): ScreenResult

    @Composable
    override fun Content() {

        val screenModel = koinScreenModel<SelectProfileImageScreenModel>()
        val state by screenModel.state.collectAsStateWithLifecycle()
        val hazeState = remember { HazeState() }
        val navigator = LocalNavigator.currentOrThrow


        val gridState = rememberLazyGridState()
        var gridItemSpan by remember { mutableStateOf<Int?>(null) }

        val scope = rememberCoroutineScope()
        val categoryToIdx = remember { mutableStateMapOf<String, Int>() }

        PullRefresh(
            refreshing = screenModel.refreshing,
            enabled = { !screenModel.refreshing },
            onRefresh = screenModel::refreshImages,
        ) {
            Scaffold(
                topBar = {
                    TopAppBarWithBottomContent(
                        title = {
                            Text("Profile Pictures")
                        },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(id = R.string.cancel)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        ),
                        bottomContent = {
                            val categoryList by remember {
                                derivedStateOf { state.orEmpty().map { it.first } }
                            }

                            LazyRow {
                                categoryList.fastForEach { category ->
                                   item(category) {
                                       ElevatedAssistChip(
                                           onClick = {
                                               scope.launch {
                                                   gridState.animateScrollToItem(
                                                       categoryToIdx[category]
                                                           ?: return@launch
                                                   )
                                               }
                                           },
                                           modifier = Modifier.padding(4.dp),
                                           label = { Text(category) }
                                       )
                                   }
                                }
                            }
                        },
                        modifier = Modifier
                            .hazeChild(hazeState),
                    )
                },
                contentWindowInsets = ScaffoldDefaults.contentWindowInsets
                    .exclude(WindowInsets.systemBars),
            ) { paddingValues ->
                if (state != null) {
                    LaunchedEffect(gridItemSpan, state) {
                        var idx = 0
                        for ((name, items) in state.orEmpty()) {
                            categoryToIdx[name] = idx
                            idx += items.lastIndex + (gridItemSpan ?: 0)
                        }
                    }

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(90.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .haze(
                                state = hazeState,
                                style = HazeDefaults
                                    .style(backgroundColor = MaterialTheme.colorScheme.background),
                            ),
                        contentPadding = paddingValues
                    ) {
                        state.orEmpty().fastForEach { (name, images) ->
                            item(
                                key = name,
                                span = {
                                    gridItemSpan = maxLineSpan
                                    GridItemSpan(maxLineSpan)
                                }
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            items(
                                images,
                                { it.bucket + it.path }
                            ) {
                                val placeHolderColor = PlaceHolderColors.rememberColorRandom(key = it.path)
                                AsyncImage(
                                    model = it,
                                    placeholder = remember { ColorPainter(placeHolderColor) },
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .clip(CircleShape)
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .colorClickable {
                                            setScreenResult(
                                                ImageResult(it.path)
                                            )
                                            navigator.pop()
                                        },
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
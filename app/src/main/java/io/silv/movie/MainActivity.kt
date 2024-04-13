package io.silv.movie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.silv.core_ui.theme.MovieTheme
import io.silv.core_ui.voyager.ScreenResultsStoreProxy
import io.silv.core_ui.voyager.ScreenResultsViewModel
import io.silv.movie.data.trailers.Trailer
import io.silv.movie.data.user.UserRepository
import io.silv.movie.presentation.CollectEventsWithLifecycle
import io.silv.movie.presentation.ContentEvent
import io.silv.movie.presentation.ListEvent
import io.silv.movie.presentation.LocalContentInteractor
import io.silv.movie.presentation.LocalListInteractor
import io.silv.movie.presentation.browse.BrowseTab
import io.silv.movie.presentation.browse.DiscoverTab
import io.silv.movie.presentation.library.LibraryTab
import io.silv.movie.presentation.media.CollapsablePlayerMinHeight
import io.silv.movie.presentation.media.CollapsablePlayerScreen
import io.silv.movie.presentation.media.CollapsableVideoAnchors
import io.silv.movie.presentation.media.rememberCollapsableVideoState
import io.silv.movie.presentation.profile.ProfileTab
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.roundToInt


val LocalGlobalNavigator = compositionLocalOf<MutableState<Navigator?>> { error("not provided") }

class MainActivity : ComponentActivity() {

    private val userRepository by inject<UserRepository>()
    private val mainScreenModel by viewModel<MainScreenModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        ScreenResultsStoreProxy.screenResultModel = getViewModel<ScreenResultsViewModel>()

        setContent {
            val currentUser by userRepository.currentUser.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val currentNavigator = remember { mutableStateOf<Navigator?>(null) }

            CompositionLocalProvider(
                LocalMainViewModelStoreOwner provides this,
                LocalUser provides currentUser,
                LocalListInteractor provides mainScreenModel.listInteractor,
                LocalContentInteractor provides mainScreenModel.contentInteractor,
                LocalGlobalNavigator provides currentNavigator,
            ) {
                val playerViewModel = getActivityViewModel<PlayerViewModel>()
                val collapsableVideoState = rememberCollapsableVideoState()

                BackHandler(
                    enabled = playerViewModel.trailerQueue.isNotEmpty()
                ) {
                    playerViewModel.clearMediaQueue()
                }

                val trailers by remember(playerViewModel.trailerQueue) {
                    derivedStateOf { playerViewModel.trailerQueue.toImmutableList() }
                }

                MovieTheme {
                    TabNavigator(LibraryTab) { tabNavigator ->
                        Surface(
                            Modifier.fillMaxSize(),
                        ) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                bottomBar = {
                                    AppBottomBar(
                                        videos = trailers,
                                        progress = { collapsableVideoState.progress },
                                        tabNavigator = tabNavigator,
                                        modifier = Modifier
                                    )
                                },
                                snackbarHost = { SnackbarHost(snackbarHostState) }
                            ) { paddingValues ->
                                val playerVisible by remember {
                                    derivedStateOf { playerViewModel.trailerQueue.isNotEmpty() }
                                }
                                val density = LocalDensity.current
                                val bottomPadding by remember {
                                    derivedStateOf {
                                        with(density) {
                                            CollapsablePlayerMinHeight - collapsableVideoState.dismissOffsetPx.toDp()
                                        }
                                    }
                                }
                                Box(
                                    Modifier
                                        .padding(paddingValues)
                                        .consumeWindowInsets(paddingValues)
                                ) {
                                    Box(
                                        Modifier
                                            .padding(
                                                bottom = animateDpAsState(
                                                    targetValue = if (playerVisible) bottomPadding else 0.dp,
                                                    label = "player-aware-padding-animated"
                                                )
                                                    .value
                                            )
                                    ) {
                                        CurrentTab()
                                    }
                                    AnimatedVisibility(
                                        visible = playerVisible,
                                        modifier = Modifier
                                            .wrapContentSize()
                                            .align(Alignment.BottomCenter),
                                        enter = slideInVertically { it } + fadeIn(),
                                        exit = fadeOut(tween(0, 0))
                                    ) {
                                        CollapsablePlayerScreen(
                                            collapsableVideoState = collapsableVideoState,
                                            onDismissRequested = playerViewModel::clearMediaQueue,
                                            playerViewModel = playerViewModel
                                        )
                                        LaunchedEffect(playerVisible) {
                                            collapsableVideoState.state.snapTo(CollapsableVideoAnchors.Start)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                CollectEventsWithLifecycle(LocalContentInteractor.current) { event ->
                    when(event)  {
                        is ContentEvent.AddToList -> {}
                        is ContentEvent.Favorite -> {}
                        is ContentEvent.RemoveFromList -> {}
                    }
                }
                CollectEventsWithLifecycle(LocalListInteractor.current) { event ->
                    when (event) {
                        is ListEvent.Copied -> {}
                        is ListEvent.Delete -> {}
                        is ListEvent.Subscribe -> {}
                        is ListEvent.VisibleChanged -> {}
                        is ListEvent.Edited -> {}
                    }
                }
            }
        }
    }
}


@Composable
fun AppBottomBar(
    videos: ImmutableList<Trailer>?,
    progress: () -> Float,
    tabNavigator: TabNavigator,
    modifier: Modifier = Modifier,
) {
    val tabs = remember {
        persistentListOf(
            LibraryTab,
            BrowseTab,
            DiscoverTab,
            ProfileTab
        )
    }
    BottomAppBar(
        modifier
            .heightIn(min = 0.dp, max = 72.dp)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val height = if (videos.isNullOrEmpty()) {
                    placeable.height
                } else {
                    val flippedProgress = (1f - progress())
                    (placeable.height * flippedProgress).roundToInt()
                }

                layout(constraints.maxWidth, height) {
                    placeable.placeRelative(0, 0)
                }
            },
    ) {
        tabs.fastForEach { tab ->
            NavigationBarItem(
                selected = tabNavigator.current == tab,
                onClick = { tabNavigator.current = tab },
                icon = {
                    Icon(
                        painter = tab.options.icon ?: return@NavigationBarItem,
                        contentDescription =  tab.options.title
                    )
                },
            )
        }
    }
}



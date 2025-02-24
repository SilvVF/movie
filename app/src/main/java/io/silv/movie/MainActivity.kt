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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.silv.core_ui.voyager.ScreenResultsViewModel
import io.silv.movie.data.model.Trailer
import io.silv.movie.data.supabase.BackendRepository
import io.silv.movie.prefrences.UiPreferences
import io.silv.movie.presentation.ContentInteractor
import io.silv.movie.presentation.ListInteractor
import io.silv.movie.presentation.LocalAppData
import io.silv.movie.presentation.LocalContentInteractor
import io.silv.movie.presentation.LocalListInteractor
import io.silv.movie.presentation.LocalUser
import io.silv.movie.presentation.LocalVideoState
import io.silv.movie.presentation.media.PlayerPresenter
import io.silv.movie.presentation.media.components.CollapsablePlayerScreen
import io.silv.movie.presentation.media.components.VideoState
import io.silv.movie.presentation.tabs.BrowseTabElement
import io.silv.movie.presentation.tabs.DiscoverTabElement
import io.silv.movie.presentation.tabs.LibraryTabElement
import io.silv.movie.presentation.tabs.ProfileTabElement
import io.silv.movie.presentation.tabs.SettingsTabElement
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {

    private val backendRepository by inject<BackendRepository>()
    private val screenResultsViewModel by viewModel<ScreenResultsViewModel>()
    private val uiPreferences by inject<UiPreferences>()
    private val listInteractor by inject<ListInteractor>()
    private val contentInteractor by inject<ContentInteractor>()
    private val playerPresenter by inject<PlayerPresenter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        screenResultsViewModel.bind()
        val appState =
            MovieAppState(
                uiPreferences,
                contentInteractor,
                listInteractor,
                screenResultsViewModel,
                this,
                lifecycleScope
            )

        splashScreen.setKeepOnScreenCondition {
            when (appState.state.value) {
                AppDataState.Loading -> true
                is AppDataState.Success -> false
            }
        }

        setContent {
            KoinAndroidContext {
                val currentUser by backendRepository.currentUser.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val appData by appState.state.collectAsStateWithLifecycle()

                val videoState = remember {
                    VideoState(
                        playerPresenter,
                        scope,
                        context,
                        appState.snackbarHostState
                    )
                }

                when (val s = appData) {
                    AppDataState.Loading -> Unit
                    is AppDataState.Success -> {
                        CompositionLocalProvider(
                            LocalUser provides currentUser,
                            LocalListInteractor provides listInteractor,
                            LocalContentInteractor provides contentInteractor,
                            LocalAppData provides s.state,
                            LocalVideoState provides videoState
                        ) {
                            MainContent(
                                appState,
                                appData = s.state,
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun MainContent(
    appState: MovieAppState,
    appData: AppData,
) {
    val videoState = LocalVideoState.current
    val state by videoState.state.collectAsStateWithLifecycle()
    val reorderState = rememberReorderableLazyListState(
        onMove = videoState::onMove,
    )

    val dismissSnackbarState =
        rememberSwipeToDismissBoxState(confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.Settled -> {
                    appState.snackbarHostState.currentSnackbarData?.dismiss()
                    true
                }

                else -> false
            }
        })
    LaunchedEffect(dismissSnackbarState.currentValue) {
        if (dismissSnackbarState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissSnackbarState.reset()
        }
    }

    BackHandler(
        enabled = state.queue.isNotEmpty()
    ) {
        videoState.clearQueue()
    }

    MovieTheme {
        TabNavigator(appData.startScreen) { tabNavigator ->
            Surface(Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        AppBottomBar(
                            videos = state.queue,
                            progress = { videoState.progress },
                            tabNavigator = tabNavigator,
                            modifier = Modifier
                        )
                    },
                    snackbarHost = {
                        SwipeToDismissBox(
                            state = dismissSnackbarState,
                            backgroundContent = {},
                            content = {
                                SnackbarHost(
                                    // classic compost
                                    // "imePadding doesnt work on M3??"
                                    hostState = appState.snackbarHostState,
                                    modifier = Modifier.imePadding()
                                )
                            },
                        )
                    }
                ) { paddingValues ->
                    Box(
                        Modifier
                            .padding(paddingValues)
                            .consumeWindowInsets(paddingValues)
                    ) {
                        val padding by animateDpAsState(
                            targetValue = if (state.queue.isNotEmpty()) {
                                videoState.bottomPadding
                            } else {
                                0.dp
                            },
                            label = "player-aware-padding-animated"
                        )

                        Box(
                            Modifier
                                .padding(bottom = padding)
                        ) {
                            CurrentTab()
                        }
                        AnimatedVisibility(
                            visible = state.queue.isNotEmpty(),
                            modifier = Modifier
                                .wrapContentSize()
                                .align(Alignment.BottomCenter),
                            enter = slideInVertically { it } + fadeIn(),
                            exit = fadeOut(tween(0, 0))
                        ) {
                            CollapsablePlayerScreen(
                                videoState = videoState,
                                state,
                                reorderState
                            )
                        }
                    }
                }
            }
        }
    }
}

private val tabs = listOf(
    LibraryTabElement,
    BrowseTabElement,
    DiscoverTabElement,
    ProfileTabElement,
    SettingsTabElement
)

@Composable
fun AppBottomBar(
    videos: List<Trailer>?,
    progress: () -> Float,
    tabNavigator: TabNavigator,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val insets = WindowInsets.systemBars.getBottom(density)

    BottomAppBar(
        modifier
            .heightIn(
                min = 0.dp,
                max = 52.dp + with(density) { insets.toDp() }
            )
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
        windowInsets = BottomAppBarDefaults.windowInsets
    ) {
        tabs.fastForEach { tab ->
            NavigationBarItem(
                selected = tabNavigator.current == tab,
                onClick = { tabNavigator.current = tab },
                icon = {
                    Icon(
                        painter = tab.options.icon ?: return@NavigationBarItem,
                        contentDescription = tab.options.title
                    )
                },
            )
        }
    }
}



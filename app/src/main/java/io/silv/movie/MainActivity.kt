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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.silv.core_ui.theme.colorScheme.CloudflareColorScheme
import io.silv.core_ui.theme.colorScheme.CottonCandyColorScheme
import io.silv.core_ui.theme.colorScheme.DoomColorScheme
import io.silv.core_ui.theme.colorScheme.GreenAppleColorScheme
import io.silv.core_ui.theme.colorScheme.LavenderColorScheme
import io.silv.core_ui.theme.colorScheme.MatrixColorScheme
import io.silv.core_ui.theme.colorScheme.MidnightDuskColorScheme
import io.silv.core_ui.theme.colorScheme.MochaColorScheme
import io.silv.core_ui.theme.colorScheme.MonetColorScheme
import io.silv.core_ui.theme.colorScheme.NordColorScheme
import io.silv.core_ui.theme.colorScheme.SapphireColorScheme
import io.silv.core_ui.theme.colorScheme.StrawberryColorScheme
import io.silv.core_ui.theme.colorScheme.TachiyomiColorScheme
import io.silv.core_ui.theme.colorScheme.TakoColorScheme
import io.silv.core_ui.theme.colorScheme.TealTurqoiseColorScheme
import io.silv.core_ui.theme.colorScheme.TidalWaveColorScheme
import io.silv.core_ui.theme.colorScheme.YinYangColorScheme
import io.silv.core_ui.theme.colorScheme.YotsubaColorScheme
import io.silv.core_ui.voyager.ScreenResultsViewModel
import io.silv.movie.data.model.Trailer
import io.silv.movie.data.supabase.BackendRepository
import io.silv.movie.prefrences.AppTheme
import io.silv.movie.prefrences.ThemeMode.DARK
import io.silv.movie.prefrences.ThemeMode.LIGHT
import io.silv.movie.prefrences.ThemeMode.SYSTEM
import io.silv.movie.presentation.LocalAppState
import io.silv.movie.presentation.LocalContentInteractor
import io.silv.movie.presentation.LocalListInteractor
import io.silv.movie.presentation.LocalMainViewModelStoreOwner
import io.silv.movie.presentation.LocalUser
import io.silv.movie.presentation.LocalVideoState
import io.silv.movie.presentation.media.StreamState
import io.silv.movie.presentation.media.components.CollapsablePlayerScreen
import io.silv.movie.presentation.media.components.VideoState
import io.silv.movie.presentation.tabs.BrowseTab
import io.silv.movie.presentation.tabs.DiscoverTab
import io.silv.movie.presentation.tabs.LibraryTab
import io.silv.movie.presentation.tabs.ProfileTab
import io.silv.movie.presentation.tabs.SettingsTab
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.currentKoinScope
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {

    private val backendRepository by inject<BackendRepository>()

    private val mainViewModel by viewModel<MainViewModel>()
    private val screenResultsViewModel by viewModel<ScreenResultsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        screenResultsViewModel.bind()

        splashScreen.setKeepOnScreenCondition {
            when (mainViewModel.state.value) {
                AppState.Loading -> true
                is AppState.Success -> false
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.contentInteractor.eventHandler(
                    mainViewModel.snackbarHostState,
                    this@MainActivity,
                    mainViewModel.navigationChannel
                )
                    .launchIn(this)
                mainViewModel.listInteractor.handleEvents(
                    mainViewModel.snackbarHostState,
                    this@MainActivity,
                    mainViewModel.navigationChannel
                )
                    .launchIn(this)
            }
        }

        setContent {
            KoinAndroidContext {
                val currentUser by backendRepository.currentUser.collectAsStateWithLifecycle()
                val appState by mainViewModel.state.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                val videoState = remember {
                    VideoState(
                        mainViewModel.playerPresenter,
                        scope,
                        context,
                        mainViewModel.snackbarHostState
                    )
                }

                val dismissSnackbarState =
                    rememberSwipeToDismissBoxState(confirmValueChange = { value ->
                        when (value) {
                            SwipeToDismissBoxValue.Settled -> {
                                mainViewModel.snackbarHostState.currentSnackbarData?.dismiss()
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

                when (val s = appState) {
                    AppState.Loading -> Unit
                    is AppState.Success -> {
                        CompositionLocalProvider(
                            LocalMainViewModelStoreOwner provides this,
                            LocalUser provides currentUser,
                            LocalListInteractor provides mainViewModel.listInteractor,
                            LocalContentInteractor provides mainViewModel.contentInteractor,
                            LocalAppState provides s.state,
                            LocalVideoState provides videoState
                        ) {
                            MainContent(
                                appData = s.state,
                                swipeToDismissBoxState = dismissSnackbarState,
                                mainViewModel = mainViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Stable
class StableParametersDefinition(val parametersDefinition: ParametersDefinition?)

@Composable
fun rememberStableParametersDefinition(
    parametersDefinition: ParametersDefinition?
): StableParametersDefinition = remember { StableParametersDefinition(parametersDefinition) }

@Composable
inline fun <reified T : ScreenModel> Screen.koin4ScreenModel(
    qualifier: Qualifier? = null,
    scope: Scope = currentKoinScope(),
    noinline parameters: ParametersDefinition? = null
): T {
    val st = parameters?.let { rememberStableParametersDefinition(parameters) }
    val tag = remember(qualifier, scope) { qualifier?.value }
    return rememberScreenModel(tag = tag) {
        scope.get(qualifier, st?.parametersDefinition)
    }
}

@Composable
private fun MainContent(
    appData: AppData,
    swipeToDismissBoxState: SwipeToDismissBoxState,
    mainViewModel: MainViewModel,
) {

    val videoState = LocalVideoState.current
    val state by videoState.state.collectAsStateWithLifecycle()
    val reorderState = rememberReorderableLazyListState(
        onMove = videoState::onMove,
        onDragEnd = { _, _ -> videoState.onDragEnd() }
    )

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
                            state = swipeToDismissBoxState,
                            backgroundContent = {},
                            content = {
                                SnackbarHost(
                                    // classic compost
                                    // "imePadding doesnt work on M3??"
                                    hostState = mainViewModel.snackbarHostState,
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


@Composable
@ReadOnlyComposable
fun getThemeColorScheme(
    appTheme: AppTheme,
    amoled: Boolean,
    dark: Boolean,
): ColorScheme {
    val colorScheme = when (appTheme) {
        AppTheme.DEFAULT -> TachiyomiColorScheme
        AppTheme.MONET -> MonetColorScheme(LocalContext.current)
        AppTheme.CLOUDFLARE -> CloudflareColorScheme
        AppTheme.COTTONCANDY -> CottonCandyColorScheme
        AppTheme.DOOM -> DoomColorScheme
        AppTheme.GREEN_APPLE -> GreenAppleColorScheme
        AppTheme.LAVENDER -> LavenderColorScheme
        AppTheme.MATRIX -> MatrixColorScheme
        AppTheme.MIDNIGHT_DUSK -> MidnightDuskColorScheme
        AppTheme.MOCHA -> MochaColorScheme
        AppTheme.SAPPHIRE -> SapphireColorScheme
        AppTheme.NORD -> NordColorScheme
        AppTheme.STRAWBERRY_DAIQUIRI -> StrawberryColorScheme
        AppTheme.TAKO -> TakoColorScheme
        AppTheme.TEALTURQUOISE -> TealTurqoiseColorScheme
        AppTheme.TIDAL_WAVE -> TidalWaveColorScheme
        AppTheme.YINYANG -> YinYangColorScheme
        AppTheme.YOTSUBA -> YotsubaColorScheme
        else -> TachiyomiColorScheme
    }
    return colorScheme.getColorScheme(
        dark,
        amoled,
    )
}

@ReadOnlyComposable
@Composable
fun isDarkTheme(): Boolean {
    val appState = LocalAppState.current
    val systemTheme = isSystemInDarkTheme()
    return when (appState.themeMode) {
        LIGHT -> false
        DARK -> true
        SYSTEM -> systemTheme
    }
}

@Composable
fun MovieTheme(
    dark: Boolean = isDarkTheme(),
    content: @Composable () -> Unit,
) {
    val appState = LocalAppState.current
    MaterialTheme(
        colorScheme = getThemeColorScheme(appState.appTheme, appState.amoled, dark),
        content = content,
    )
}

private val tabs = listOf(
    LibraryTab,
    BrowseTab,
    DiscoverTab,
    ProfileTab,
    SettingsTab
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



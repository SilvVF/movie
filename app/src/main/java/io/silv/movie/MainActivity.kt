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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.Navigator
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
import io.silv.core_ui.voyager.ScreenResultsStoreProxy
import io.silv.core_ui.voyager.ScreenResultsViewModel
import io.silv.movie.data.prefrences.AppTheme
import io.silv.movie.data.prefrences.ThemeMode.DARK
import io.silv.movie.data.prefrences.ThemeMode.LIGHT
import io.silv.movie.data.prefrences.ThemeMode.SYSTEM
import io.silv.movie.data.prefrences.UiPreferences
import io.silv.movie.data.prefrences.core.getOrDefaultBlocking
import io.silv.movie.data.trailers.Trailer
import io.silv.movie.data.user.UserRepository
import io.silv.movie.presentation.LocalContentInteractor
import io.silv.movie.presentation.LocalListInteractor
import io.silv.movie.presentation.browse.BrowseTab
import io.silv.movie.presentation.browse.DiscoverTab
import io.silv.movie.presentation.collectAsState
import io.silv.movie.presentation.library.LibraryTab
import io.silv.movie.presentation.media.CollapsablePlayerMinHeight
import io.silv.movie.presentation.media.CollapsablePlayerScreen
import io.silv.movie.presentation.media.CollapsableVideoAnchors
import io.silv.movie.presentation.media.rememberCollapsableVideoState
import io.silv.movie.presentation.profile.ProfileTab
import io.silv.movie.presentation.settings.SettingsTab
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject
import kotlin.math.roundToInt


object Nav {

    var current by mutableStateOf<Navigator?>(null)
        private set

    fun setNav(navigator: Navigator) {
        current = navigator
    }

    fun clear() { current = null }
}

class MainActivity : ComponentActivity() {

    private val userRepository by inject<UserRepository>()
    private val mainScreenModel by viewModel<MainScreenModel>()
    private val uiPreferences by inject<UiPreferences>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        ScreenResultsStoreProxy.screenResultModel = getViewModel<ScreenResultsViewModel>()

        setContent {
            val currentUser by userRepository.currentUser.collectAsStateWithLifecycle()

            LifecycleEventEffect(event = Lifecycle.Event.ON_STOP) {
                Nav.clear()
            }

            CompositionLocalProvider(
                LocalMainViewModelStoreOwner provides this,
                LocalUser provides currentUser,
                LocalListInteractor provides mainScreenModel.listInteractor,
                LocalContentInteractor provides mainScreenModel.contentInteractor,
            ) {
                val playerViewModel = getActivityViewModel<PlayerViewModel>()
                val listInteractor = LocalListInteractor.current
                val contentInteractor = LocalContentInteractor.current
                val collapsableVideoState = rememberCollapsableVideoState()
                val snackbarHostState = remember { SnackbarHostState() }

                val dismissSnackbarState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
                    if (value != SwipeToDismissBoxValue.Settled) {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        true
                    } else {
                        false
                    }
                })

                LaunchedEffect(dismissSnackbarState.currentValue) {
                    if (dismissSnackbarState.currentValue != SwipeToDismissBoxValue.Settled) {
                        dismissSnackbarState.reset()
                    }
                }

                BackHandler(
                    enabled = playerViewModel.trailerQueue.isNotEmpty()
                ) {
                    playerViewModel.clearMediaQueue()
                }

                val trailers by remember(playerViewModel.trailerQueue) {
                    derivedStateOf { playerViewModel.trailerQueue.toImmutableList() }
                }

                MovieTheme {
                    TabNavigator(uiPreferences.startScreen().getOrDefaultBlocking().tab) { tabNavigator ->
                        Surface(Modifier.fillMaxSize(),) {
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
                                snackbarHost = {
                                    SwipeToDismissBox(
                                        state = dismissSnackbarState,
                                        backgroundContent = {},
                                        content = {
                                            SnackbarHost(
                                                // classic compost
                                                // "imePadding doesnt work on M3??"
                                                hostState = snackbarHostState, modifier = Modifier.imePadding()
                                            )
                                        },
                                    )
                                }
                            ) { paddingValues ->
                                val trailerPlayerVisible by remember {
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
                                                    targetValue = if (trailerPlayerVisible) bottomPadding else 0.dp,
                                                    label = "player-aware-padding-animated"
                                                )
                                                    .value
                                            )
                                    ) {
                                        CurrentTab()
                                    }
                                    AnimatedVisibility(
                                        visible = trailerPlayerVisible,
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
                                    }
                                    LaunchedEffect(trailerPlayerVisible) {
                                        collapsableVideoState.state.snapTo(CollapsableVideoAnchors.Start)
                                    }
                                }
                            }
                        }
                        HandleItemEvents(
                            contentInteractor = contentInteractor,
                            snackbarHostState = snackbarHostState,
                            navigator = { Nav.current}
                        )
                        HandleListEvents(
                            listInteractor = listInteractor,
                            snackbarHostState = snackbarHostState,
                            navigator = { Nav.current }
                        )
                    }
                }
            }
        }
    }
}


@Composable
@ReadOnlyComposable
private fun getThemeColorScheme(
    appTheme: AppTheme?,
    amoled: Boolean?,
    dark: Boolean?,
    uiPreferences: UiPreferences,
): ColorScheme {
    val colorScheme = when (appTheme ?: uiPreferences.appTheme().getOrDefaultBlocking()) {
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
         dark ?: when(uiPreferences.themeMode().getOrDefaultBlocking()) {
            LIGHT -> false
            DARK -> true
            SYSTEM -> isSystemInDarkTheme()
        },
        amoled ?: uiPreferences.themeDarkAmoled().getOrDefaultBlocking(),
    )
}

@Composable
fun MovieTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val uiPreferences = koinInject<UiPreferences>()
    val dark = when(uiPreferences.themeMode().collectAsState().value) {
        LIGHT -> false
        DARK -> true
        SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = getThemeColorScheme(appTheme, amoled, dark, uiPreferences),
        content = content,
    )
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
            ProfileTab,
            SettingsTab
        )
    }
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
                        contentDescription =  tab.options.title
                    )
                },
            )
        }
    }
}



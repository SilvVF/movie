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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.silv.core_ui.theme.MovieTheme
import io.silv.core_ui.voyager.ScreenResultsStoreProxy
import io.silv.core_ui.voyager.ScreenResultsViewModel
import io.silv.movie.data.trailers.Trailer
import io.silv.movie.data.user.User
import io.silv.movie.data.user.UserRepository
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
import org.koin.androidx.compose.defaultExtras
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.androidx.viewmodel.resolveViewModel
import org.koin.compose.currentKoinScope
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import kotlin.math.roundToInt


val LocalMainViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner> { error("not provided") }

@OptIn(KoinInternalApi::class)
@Composable
inline fun <reified T : ViewModel> getActivityViewModel(
    qualifier: Qualifier? = null,
    key: String? = null,
    scope: Scope = currentKoinScope(),
    noinline parameters: ParametersDefinition? = null,
): T {

    val viewModelStoreOwner = checkNotNull(LocalMainViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalMainViewModelStoreOwner"
    }
    val extras = defaultExtras(viewModelStoreOwner)

    return resolveViewModel(
        T::class, viewModelStoreOwner.viewModelStore, key, extras, qualifier, scope, parameters
    )
}

val LocalUser = compositionLocalOf<User?> { error("no user provided") }

@Composable
fun User?.rememberProfileImageData(): UserProfileImageData? {
    val currentUser = LocalUser.current
    return remember(this?.profileImage, currentUser?.profileImage) {
        this?.let {
            UserProfileImageData(
                userId = it.userId,
                isUserMe = it.userId == currentUser?.userId,
                path = it.profileImage
            )
        }
    }
}

class MainActivity : ComponentActivity() {

    private val userRepository by inject<UserRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        ScreenResultsStoreProxy.screenResultModel = getViewModel<ScreenResultsViewModel>()


        setContent {
            val currentUser by userRepository.currentUser.collectAsStateWithLifecycle()

            CompositionLocalProvider(
                LocalMainViewModelStoreOwner provides this,
                LocalUser provides currentUser
            ) {
                val mainScreenModel = getActivityViewModel<PlayerViewModel>()
                val collapsableVideoState = rememberCollapsableVideoState()

                BackHandler(
                    enabled = mainScreenModel.trailerQueue.isNotEmpty()
                ) {
                    mainScreenModel.clearMediaQueue()
                }

                val trailers by remember(mainScreenModel.trailerQueue) {
                    derivedStateOf { mainScreenModel.trailerQueue.toImmutableList() }
                }

                MovieTheme {
                    TabNavigator(LibraryTab) { tabNavigator ->
                        Surface(Modifier.fillMaxSize()) {
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
                            ) { paddingValues ->
                                val playerVisible by remember {
                                    derivedStateOf { mainScreenModel.trailerQueue.isNotEmpty() }
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
                                            onDismissRequested = mainScreenModel::clearMediaQueue,
                                            playerViewModel = mainScreenModel
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



package io.silv.movie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.silv.core_ui.theme.MovieTheme
import io.silv.core_ui.voyager.ScreenResultsStoreProxy
import io.silv.core_ui.voyager.ScreenResultsViewModel
import io.silv.movie.data.trailers.Trailer
import io.silv.movie.presentation.home.HomeTab
import io.silv.movie.presentation.library.LibraryTab
import io.silv.movie.presentation.media.CollapsablePlayerScreen
import io.silv.movie.presentation.media.rememberCollapsableVideoState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.defaultExtras
import org.koin.androidx.viewmodel.ext.android.viewModel
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val screenResultsModel  by viewModel<ScreenResultsViewModel>()

        ScreenResultsStoreProxy.screenResultModel = screenResultsModel

        setContent {
            CompositionLocalProvider(
                LocalMainViewModelStoreOwner provides this,
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
                    TabNavigator(HomeTab) { tabNavigator ->
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
                                Box(
                                    Modifier
                                        .padding(paddingValues)
                                        .consumeWindowInsets(paddingValues)
                                ) {
                                    CurrentTab()

                                    AnimatedVisibility(
                                        visible = mainScreenModel.trailerQueue.isNotEmpty(),
                                        modifier = Modifier
                                            .wrapContentSize()
                                            .align(Alignment.BottomCenter),
                                        enter = slideInVertically { it } + fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        CollapsablePlayerScreen(
                                            collapsableVideoState = collapsableVideoState,
                                            onDismissRequested = mainScreenModel::clearMediaQueue,
                                            playerViewModel = mainScreenModel
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                DisposableEffect(collapsableVideoState) {
                    mainScreenModel.collapsableVideoState = collapsableVideoState
                    onDispose {
                        mainScreenModel.collapsableVideoState = null
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
            HomeTab,
            LibraryTab
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



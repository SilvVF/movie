package io.silv.movie

import android.content.Context
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import io.silv.core_ui.theme.MovieTheme
import io.silv.core_ui.voyager.ScreenResultsStoreProxy
import io.silv.core_ui.voyager.ScreenResultsViewModel
import io.silv.movie.data.trailers.Trailer
import io.silv.movie.data.user.UserRepository
import io.silv.movie.presentation.CollectEventsWithLifecycle
import io.silv.movie.presentation.ContentEvent
import io.silv.movie.presentation.ContentInteractor
import io.silv.movie.presentation.ListEvent
import io.silv.movie.presentation.ListInteractor
import io.silv.movie.presentation.LocalContentInteractor
import io.silv.movie.presentation.LocalListInteractor
import io.silv.movie.presentation.browse.BrowseTab
import io.silv.movie.presentation.browse.DiscoverTab
import io.silv.movie.presentation.library.LibraryTab
import io.silv.movie.presentation.library.screens.ListViewScreen
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
private fun HandleListEvents(
    listInteractor: ListInteractor,
    snackbarHostState: SnackbarHostState,
    navigator: () -> Navigator?,
    context: Context = LocalContext.current
) {
    CollectEventsWithLifecycle(listInteractor) { event ->
        when (event) {
            is ListEvent.Copied -> {
                if (event.success) {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.copied_list, event.list.name),
                        actionLabel = context.getString(R.string.take_me_there),
                        duration = SnackbarDuration.Short,
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed ->
                            navigator()?.push(
                                ListViewScreen(
                                    event.list.id,
                                    event.list.supabaseId.orEmpty()
                                )
                            )
                    }
                } else {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.copied_list_failed, event.list.name),
                        actionLabel = context.getString(R.string.retry),
                        duration = SnackbarDuration.Short,
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed -> listInteractor.copyList(event.list)
                    }
                }
            }

            is ListEvent.Delete -> {
                if (event.success) {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.deleted_list, event.list.name),
                        duration = SnackbarDuration.Short
                    )
                } else {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.delete_list_failed, event.list.name),
                        actionLabel = context.getString(R.string.retry),
                        duration = SnackbarDuration.Short,
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed -> listInteractor.deleteList(event.list)
                    }
                }
            }

            is ListEvent.Subscribe -> {
                if (event.success) {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.subscribed_to_list, event.list.name),
                        duration = SnackbarDuration.Short
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed ->
                            navigator()?.push(
                                ListViewScreen(
                                    event.list.id,
                                    event.list.supabaseId.orEmpty()
                                )
                            )
                    }
                } else {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(
                            R.string.subscribe_to_list_failed,
                            event.list.name
                        ),
                        actionLabel = context.getString(R.string.retry),
                        duration = SnackbarDuration.Short,
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed -> listInteractor.subscribeToList(event.list)
                    }
                }
            }

            is ListEvent.VisibleChanged -> {
                if (event.success) {
                    val result = snackbarHostState.showSnackbar(
                        message = if (event.list.public) {
                            context.getString(R.string.made_list_public)
                        } else {
                            context.getString(R.string.made_list_private)
                        },
                        actionLabel = context.getString(R.string.undo),
                        duration = SnackbarDuration.Short
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed ->
                            listInteractor.editList(event.list) { it.copy(public = !it.public) }
                    }
                } else {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(
                            R.string.visiblity_change_failed,
                            event.list.name
                        ),
                        actionLabel = context.getString(R.string.retry),
                        duration = SnackbarDuration.Short,
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed ->
                            listInteractor.editList(event.list) { it.copy(public = !it.public) }
                    }
                }
            }

            is ListEvent.Edited -> {
                if (event.success) {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.list_edited),
                        actionLabel = context.getString(R.string.undo),
                        duration = SnackbarDuration.Short
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed ->
                            listInteractor.editList(event.new) { event.original }
                    }
                } else {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.edit_failed, event.original.name),
                        actionLabel = context.getString(R.string.retry),
                        duration = SnackbarDuration.Short,
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed ->
                            listInteractor.editList(event.original) { event.new }
                    }
                }
            }
        }
    }
}

@Composable
private fun HandleItemEvents(
    contentInteractor: ContentInteractor,
    snackbarHostState: SnackbarHostState,
    navigator: () -> Navigator?,
    context: Context = LocalContext.current
) {
    CollectEventsWithLifecycle(contentInteractor) { event ->
        when(event)  {
            is ContentEvent.AddToList -> {
                if (event.success) {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.added_to_list, event.item.title, event.list.name),
                        actionLabel = context.getString(R.string.undo),
                        duration = SnackbarDuration.Short,
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed ->
                            contentInteractor.removeFromList(event.list, event.item)
                    }
                } else {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.add_to_list_failed, event.item.title, event.list.name),
                        actionLabel = context.getString(R.string.retry),
                        duration = SnackbarDuration.Short,
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed ->
                            contentInteractor.addToList(event.list, event.item)
                    }
                }
            }
            is ContentEvent.Favorite -> {
                if (event.success) {
                    val result = snackbarHostState.showSnackbar(
                        message = if (event.item.favorite) {
                            context.getString(R.string.added_to_favorites, event.item.title)
                        } else context.getString(R.string.removed_from_favorites, event.item.title),
                        actionLabel = context.getString(R.string.undo),
                        duration = SnackbarDuration.Short,
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed ->
                            contentInteractor.toggleFavorite(event.item)
                    }
                } else {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.failed_to_change_favorite, event.item.title),
                        actionLabel = context.getString(R.string.retry),
                        duration = SnackbarDuration.Short,
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed ->
                            contentInteractor.toggleFavorite(event.item)
                    }
                }
            }
            is ContentEvent.RemoveFromList -> {
                if (event.success) {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.removed_from_list, event.item.title, event.list.name),
                        actionLabel = context.getString(R.string.undo),
                        duration = SnackbarDuration.Short,
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed ->
                            contentInteractor.addToList(event.list, event.item)
                    }
                } else {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.remove_from_list_failed, event.item.title, event.list.name),
                        actionLabel = context.getString(R.string.retry),
                        duration = SnackbarDuration.Short,
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed ->
                            contentInteractor.removeFromList(event.list, event.item)
                    }
                }
            }

            is ContentEvent.AddToAnotherList -> {
                if (event.success) {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.added_to_list, event.item.title, event.list.name),
                        actionLabel = context.getString(R.string.take_me_there),
                        duration = SnackbarDuration.Short,
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed ->
                            navigator()?.push(ListViewScreen(event.list.id, event.list.supabaseId.orEmpty()))
                    }
                } else {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.add_to_list_failed, event.item.title, event.list.name),
                        actionLabel = context.getString(R.string.retry),
                        duration = SnackbarDuration.Short,
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed ->
                            contentInteractor.addToList(event.list, event.item)
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



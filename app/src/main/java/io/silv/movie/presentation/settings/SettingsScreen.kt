package io.silv.movie.presentation.settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.core_ui.components.topbar.AppBar
import io.silv.core_ui.components.topbar.AppBarActions
import io.silv.movie.MovieTheme
import io.silv.movie.R
import io.silv.movie.isDarkTheme
import io.silv.movie.presentation.settings.screens.SettingsAppearanceScreen
import io.silv.movie.presentation.settings.screens.SettingsSearchScreen
import io.silv.movie.presentation.settings.screens.SettingsStorageeScreen
import io.silv.movie.presentation.settings.widgets.TextPreferenceWidget

data object SettingsMainScreen : Screen {
    private fun readResolve(): Any = SettingsMainScreen

    @Composable
    override fun Content() {
        Content(twoPane = false)
    }

    @Composable
    fun Content(twoPane: Boolean) {
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.currentOrThrow
        val topBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState)

        MovieTheme {
            Scaffold(
                topBar = {
                    AppBar(
                        title = stringResource(R.string.label_settings),
                        navigateUp = backPress::invoke,
                        actions = {
                            AppBarActions(
                                listOf(
                                    AppBar.Action(
                                        title = stringResource(R.string.action_search),
                                        icon = Icons.Outlined.Search,
                                        onClick = {
                                            navigator.navigate(
                                                SettingsSearchScreen(),
                                                twoPane
                                            )
                                        },
                                    ),
                                ),
                            )
                        },
                        isDarkTheme = isDarkTheme(),
                        scrollBehavior = scrollBehavior,
                    )
                },
                content = { contentPadding ->
                    val state = rememberLazyListState()

                    LazyColumn(
                        state = state,
                        contentPadding = contentPadding,
                    ) {
                        itemsIndexed(
                            items = items,
                            key = { _, item -> item.hashCode() },
                        ) { index, item ->
                            TextPreferenceWidget(
                                modifier = Modifier,
                                title = stringResource(item.titleRes),
                                subtitle = item.formatSubtitle(),
                                icon = item.icon,
                                onPreferenceClick = {
                                    navigator.navigate(
                                        item.screen,
                                        twoPane
                                    )
                                },
                            )
                        }
                    }
                },
            )
        }
    }

    private fun Navigator.navigate(screen: Screen, twoPane: Boolean) {
        if (twoPane) replaceAll(screen) else push(screen)
    }

    private data class Item(
        val titleRes: StringResource,
        val subtitleRes: StringResource? = null,
        val formatSubtitle: @Composable () -> String? = { subtitleRes?.let { stringResource(it) } },
        val icon: ImageVector,
        val screen: Screen,
    )

    private val items = listOf(
        Item(
            titleRes = R.string.pref_category_appearance,
            subtitleRes = R.string.pref_appearance_summary,
            icon = Icons.Outlined.Palette,
            screen = SettingsAppearanceScreen,
        ),
        Item(
            titleRes = R.string.storage,
            subtitleRes = R.string.storage_summary,
            icon = Icons.Outlined.Storage,
            screen = SettingsStorageeScreen,
        ),
    )
}
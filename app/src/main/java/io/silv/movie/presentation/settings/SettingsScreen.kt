package io.silv.movie.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.core_ui.components.topbar.AppBar
import io.silv.core_ui.components.topbar.AppBarActions
import io.silv.movie.R
import io.silv.movie.presentation.settings.screens.SettingsAppearanceScreen
import io.silv.movie.presentation.settings.screens.SettingsSearchScreen
import io.silv.movie.presentation.settings.screens.SettingsStorageeScreen
import io.silv.movie.presentation.settings.widgets.TextPreferenceWidget
import kotlinx.collections.immutable.persistentListOf

data object SettingsMainScreen : Screen {

    @Composable
    override fun Content() {
        Content(twoPane = false)
    }

    @Composable
    private fun getPalerSurface(): Color {
        val surface = MaterialTheme.colorScheme.surface
        val dark = isSystemInDarkTheme()
        return remember(surface, dark) {
            val arr = FloatArray(3)
            ColorUtils.colorToHSL(surface.toArgb(), arr)
            arr[2] = if (dark) {
                arr[2] - 0.05f
            } else {
                arr[2] + 0.02f
            }.coerceIn(0f, 1f)
            Color.hsl(arr[0], arr[1], arr[2])
        }
    }

    @Composable
    fun Content(twoPane: Boolean) {
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.currentOrThrow
        val containerColor = if (twoPane) getPalerSurface() else MaterialTheme.colorScheme.surface
        val topBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState)

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(R.string.label_settings),
                    navigateUp = backPress::invoke,
                    actions = {
                        AppBarActions(
                            persistentListOf(
                                AppBar.Action(
                                    title = stringResource(R.string.action_search),
                                    icon = Icons.Outlined.Search,
                                    onClick = { navigator.navigate(SettingsSearchScreen(), twoPane) },
                                ),
                            ),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            containerColor = containerColor,
            content = { contentPadding ->
                val state = rememberLazyListState()
                val indexSelected = if (twoPane) {
                    items.indexOfFirst { it.screen::class == navigator.items.first()::class }
                        .also {
                            LaunchedEffect(Unit) {
                                state.animateScrollToItem(it)
                                if (it > 0) {
                                    // Lift scroll
                                    topBarState.contentOffset = topBarState.heightOffsetLimit
                                }
                            }
                        }
                } else {
                    null
                }

                LazyColumn(
                    state = state,
                    contentPadding = contentPadding,
                ) {
                    itemsIndexed(
                        items = items,
                        key = { _, item -> item.hashCode() },
                    ) { index, item ->
                        val selected = indexSelected == index
                        var modifier: Modifier = Modifier
                        var contentColor = LocalContentColor.current
                        if (twoPane) {
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .then(
                                    if (selected) {
                                        Modifier.background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                            if (selected) {
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        }
                        CompositionLocalProvider(LocalContentColor provides contentColor) {
                            TextPreferenceWidget(
                                modifier = modifier,
                                title = stringResource(item.titleRes),
                                subtitle = item.formatSubtitle(),
                                icon = item.icon,
                                onPreferenceClick = { navigator.navigate(item.screen, twoPane) },
                            )
                        }
                    }
                }
            },
        )
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
//        Item(
//            titleRes = MR.strings.pref_category_player,
//            subtitleRes = MR.strings.pref_player_summary,
//            icon = Icons.Outlined.PlayCircleOutline,
//            screen = SettingsPlayerScreen,
//        ),
//        Item(
//            titleRes = MR.strings.pref_category_reader,
//            subtitleRes = MR.strings.pref_reader_summary,
//            icon = Icons.AutoMirrored.Outlined.ChromeReaderMode,
//            screen = SettingsReaderScreen,
//        ),
//        Item(
//            titleRes = MR.strings.pref_category_downloads,
//            subtitleRes = MR.strings.pref_downloads_summary,
//            icon = Icons.Outlined.GetApp,
//            screen = SettingsDownloadScreen,
//        ),
//        Item(
//            titleRes = MR.strings.pref_category_tracking,
//            subtitleRes = MR.strings.pref_tracking_summary,
//            icon = Icons.Outlined.Sync,
//            screen = SettingsTrackingScreen,
//        ),
//        Item(
//            titleRes = MR.strings.browse,
//            subtitleRes = MR.strings.pref_browse_summary,
//            icon = Icons.Outlined.Explore,
//            screen = SettingsBrowseScreen,
//        ),
//        Item(
//            titleRes = MR.strings.label_data_storage,
//            subtitleRes = MR.strings.pref_backup_summary,
//            icon = Icons.Outlined.Storage,
//            screen = SettingsDataScreen,
//        ),
//        Item(
//            titleRes = MR.strings.pref_category_security,
//            subtitleRes = MR.strings.pref_security_summary,
//            icon = Icons.Outlined.Security,
//            screen = SettingsSecurityScreen,
//        ),
//        Item(
//            titleRes = MR.strings.pref_category_advanced,
//            subtitleRes = MR.strings.pref_advanced_summary,
//            icon = Icons.Outlined.Code,
//            screen = SettingsAdvancedScreen,
//        ),
//        Item(
//            titleRes = MR.strings.pref_category_about,
//            formatSubtitle = {
//                "${stringResource(MR.strings.app_name)} ${AboutScreen.getVersionName(
//                    withBuildDate = false,
//                )}"
//            },
//            icon = Icons.Outlined.Info,
//            screen = AboutScreen,
//        ),
    )
}
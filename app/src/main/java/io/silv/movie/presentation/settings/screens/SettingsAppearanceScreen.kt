package io.silv.movie.presentation.settings.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.silv.core_ui.components.ItemCover
import io.silv.movie.MovieTheme
import io.silv.movie.R
import io.silv.movie.data.prefrences.AppTheme
import io.silv.movie.data.prefrences.DeviceUtil
import io.silv.movie.data.prefrences.StartScreen
import io.silv.movie.data.prefrences.TabletUiMode
import io.silv.movie.data.prefrences.ThemeMode
import io.silv.movie.data.prefrences.UiPreferences
import io.silv.movie.presentation.LocalAppState
import io.silv.movie.presentation.settings.Preference
import io.silv.movie.presentation.settings.SearchableSettings
import io.silv.movie.presentation.settings.widgets.BasePreferenceWidget
import io.silv.movie.presentation.settings.widgets.PrefsHorizontalPadding
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.time.Instant


data object SettingsAppearanceScreen: SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = R.string.pref_category_appearance

    @Composable
    override fun getPreferences(): List<Preference> {
        val uiPreferences = koinInject<UiPreferences>()

        return listOf(
            getThemeGroup(uiPreferences = uiPreferences),
            getDisplayGroup(uiPreferences = uiPreferences),
        )
    }

    @Composable
    private fun getThemeGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val appState = LocalAppState.current

        val scope = rememberCoroutineScope()

        val themeModePref = remember { uiPreferences.themeMode() }
        val themeMode = appState.themeMode

        val appThemePref  = remember { uiPreferences.appTheme() }
        val appTheme = appState.appTheme

        val amoledPref   = remember { uiPreferences.themeDarkAmoled() }
        val amoled = appState.amoled

        val sharedElementTransitionsPref   = remember { uiPreferences.sharedElementTransitions() }
        val sharedElementTransitions = appState.sharedElementTransitions

        return Preference.PreferenceGroup(
            title = stringResource(R.string.pref_category_theme),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(R.string.pref_app_theme),
                ) {
                    Column {
                        AppThemeModePreferenceWidget(
                            value = themeMode,
                            onItemClick = {
                                scope.launch {
                                    themeModePref.set(it)
                                }
                            },
                        )
                        AppThemePreferenceWidget(
                            value = appTheme,
                            amoled = amoled,
                            onItemClick = {
                                scope.launch {
                                    appThemePref.set(it)
                                }
                            },
                        )
                    }
                },
                Preference.PreferenceItem.SwitchPreference(
                    pref = amoledPref,
                    title = stringResource(R.string.pref_dark_theme_pure_black),
                    enabled = themeMode != ThemeMode.LIGHT,
                    onValueChanged = {
                        amoledPref.set(it)
                        amoledPref.get()
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = sharedElementTransitionsPref,
                    title = stringResource(R.string.pref_shared_element_transitions),
                    enabled = true,
                    onValueChanged = {
                        sharedElementTransitionsPref.set(it)
                        sharedElementTransitionsPref.get()
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getDisplayGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val fmt = LocalAppState.current.dateFormat

        val now = remember { Instant.now().toEpochMilli() }
        val formattedNow = remember(fmt) {
            fmt.format(now)
        }
        val dateFormat = remember(uiPreferences) { uiPreferences.dateFormat() }
        val startScreen = remember(uiPreferences) { uiPreferences.startScreen() }
        val tabletUiMode = remember(uiPreferences) { uiPreferences.tabletUiMode() }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.pref_category_display),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    pref = tabletUiMode,
                    title = stringResource(R.string.pref_tablet_ui_mode),
                    entries = TabletUiMode.entries
                        .associateWith { stringResource(it.titleRes) }
                        .toImmutableMap(),
                    onValueChanged = {
                        Toast.makeText(context, R.string.requires_app_restart, Toast.LENGTH_SHORT).show()
                        true
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = startScreen,
                    title = stringResource(R.string.pref_start_screen),
                    entries = StartScreen.entries
                        .associateWith { stringResource(it.titleRes) }
                        .toImmutableMap(),
                    onValueChanged = {
                        Toast.makeText(context, R.string.requires_app_restart, Toast.LENGTH_SHORT).show()
                        true
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = dateFormat,
                    title = stringResource(R.string.pref_date_format),
                    entries = DateFormats
                        .associateWith {
                            val formattedDate = UiPreferences.dateFormat(it).format(now)
                            "${it.ifEmpty { stringResource(R.string.label_default) }} ($formattedDate)"
                        }
                        .toImmutableMap(),
                    onValueChanged = {
                        dateFormat.set(it)
                        true
                    }
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = uiPreferences.relativeTime(),
                    title = stringResource(R.string.pref_relative_format),
                    subtitle = stringResource(
                        R.string.pref_relative_format_summary,
                        stringResource(R.string.relative_time_today),
                        formattedNow,
                    ),
                ),
            ),
        )
    }
}

private val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    mapOf(
        ThemeMode.SYSTEM to R.string.theme_system,
        ThemeMode.LIGHT to R.string.theme_light,
        ThemeMode.DARK to R.string.theme_dark,
    )
} else {
    mapOf(
        ThemeMode.LIGHT to R.string.theme_light,
        ThemeMode.DARK to R.string.theme_dark,
    )
}

@Composable
internal fun AppThemeModePreferenceWidget(
    value: ThemeMode,
    onItemClick: (ThemeMode) -> Unit,
) {
    BasePreferenceWidget(
        subcomponent = {
            MultiChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PrefsHorizontalPadding),
            ) {
                options.onEachIndexed { index, (mode, labelRes) ->
                    SegmentedButton(
                        checked = mode == value,
                        onCheckedChange = { onItemClick(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index,
                            options.size,
                        ),
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
            }
        },
    )
}

@Composable
internal fun AppThemePreferenceWidget(
    value: AppTheme,
    amoled: Boolean,
    onItemClick: (AppTheme) -> Unit,
) {
    BasePreferenceWidget(
        subcomponent = {
            AppThemesList(
                currentTheme = value,
                amoled = amoled,
                onItemClick = onItemClick,
            )
        },
    )
}

@Composable
private fun AppThemesList(
    currentTheme: AppTheme,
    amoled: Boolean,
    onItemClick: (AppTheme) -> Unit,
) {
    val appThemes = remember {
        AppTheme.entries
            .filterNot { it.titleRes == null || (it == AppTheme.MONET && !DeviceUtil.isDynamicColorAvailable) }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = PrefsHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(
            items = appThemes,
            key = { it.name },
        ) { appTheme ->
            Column(
                modifier = Modifier
                    .width(114.dp)
                    .padding(top = 8.dp),
            ) {
                MovieTheme(
                    appTheme = appTheme,
                    amoled = amoled,
                    dark = when(LocalAppState.current.themeMode) {
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK -> true
                        ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    }
                ) {
                    AppThemePreviewItem(
                        selected = currentTheme == appTheme,
                        onClick = {
                            onItemClick(appTheme)
                        },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(appTheme.titleRes!!),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.78f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    minLines = 2,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun AppThemePreviewItem(
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .border(
                width = 4.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    DividerDefaults.color
                },
                shape = RoundedCornerShape(17.dp),
            )
            .padding(4.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick),
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .weight(0.7f)
                    .padding(end = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = MaterialTheme.shapes.small,
                    ),
            )

            Box(
                modifier = Modifier.weight(0.3f),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.selected),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Cover
        Box(
            modifier = Modifier
                .padding(start = 8.dp, top = 2.dp)
                .background(
                    color = DividerDefaults.color,
                    shape = MaterialTheme.shapes.small,
                )
                .fillMaxWidth(0.5f)
                .aspectRatio(ItemCover.Book.ratio),
        ) {
            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .size(width = 24.dp, height = 16.dp)
                    .clip(RoundedCornerShape(5.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(12.dp)
                        .background(MaterialTheme.colorScheme.tertiary),
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(12.dp)
                        .background(MaterialTheme.colorScheme.secondary),
                )
            }
        }

        // Bottom bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                tonalElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .alpha(0.6f)
                            .height(17.dp)
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = MaterialTheme.shapes.small,
                            ),
                    )
                }
            }
        }
    }
}


private val DateFormats = listOf(
    "", // Default
    "MM/dd/yy",
    "dd/MM/yy",
    "yyyy-MM-dd",
    "dd MMM yyyy",
    "MMM dd, yyyy",
)
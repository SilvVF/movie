package io.silv.movie.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.silv.core_ui.components.topbar.AppBar
import io.silv.core_ui.theme.getColorScheme
import io.silv.movie.AppData
import io.silv.movie.isDarkTheme
import io.silv.movie.prefrences.ThemeMode
import io.silv.movie.presentation.LocalAppState
import io.silv.movie.presentation.collectAsState
import io.silv.movie.presentation.settings.widgets.EditTextPreferenceWidget
import io.silv.movie.presentation.settings.widgets.FloatSliderItem
import io.silv.movie.presentation.settings.widgets.InfoWidget
import io.silv.movie.presentation.settings.widgets.ListPreferenceWidget
import io.silv.movie.presentation.settings.widgets.MultiSelectListPreferenceWidget
import io.silv.movie.presentation.settings.widgets.SliderItem
import io.silv.movie.presentation.settings.widgets.SwitchPreferenceWidget
import io.silv.movie.presentation.settings.widgets.SwitchPreferenceWidgetPlaceholder
import io.silv.movie.presentation.settings.widgets.TextPreferencePlaceHolder
import io.silv.movie.presentation.settings.widgets.TextPreferenceWidget
import kotlinx.coroutines.launch


@Composable
fun PreferenceScaffold(
    titleRes: StringResource,
    actions: @Composable RowScope.() -> Unit = {},
    onBackPressed: (() -> Unit)? = null,
    appData: AppData = LocalAppState.current,
    itemsProvider: @Composable () -> List<Preference>,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        containerColor = getColorScheme(appData.themeMode != ThemeMode.LIGHT, null, null).surface,
        contentColor = LocalContentColor.current,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppBar(
                title = stringResource(titleRes),
                navigateUp = onBackPressed,
                actions = actions,
                scrollBehavior = scrollBehavior,
                isDarkTheme = isDarkTheme()
            )
        },
        content = { contentPadding ->
            PreferenceScreen(
                items = itemsProvider(),
                contentPadding = contentPadding,
            )
        },
    )
}

val LocalPreferenceHighlighted = compositionLocalOf(structuralEqualityPolicy()) { false }
val LocalPreferenceMinHeight = compositionLocalOf(structuralEqualityPolicy()) { 56.dp }

@Composable
fun StatusWrapper(
    item: Preference.PreferenceItem<*>,
    highlightKey: String?,
    content: @Composable () -> Unit,
) {
    val enabled = item.enabled
    val highlighted = item.title == highlightKey
    AnimatedVisibility(
        visible = enabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        content = {
            CompositionLocalProvider(
                LocalPreferenceHighlighted provides highlighted,
                content = content,
            )
        },
    )
}

@Composable
internal fun PreferenceItem(
    item: Preference.PreferenceItem<*>,
    highlightKey: String?,
) {
    val scope = rememberCoroutineScope()
    StatusWrapper(
        item = item,
        highlightKey = highlightKey,
    ) {
        when (item) {
            is Preference.PreferenceItem.SwitchPreference -> {
                val value by item.pref.collectAsState()
                value?.let {
                    SwitchPreferenceWidget(
                        title = item.title,
                        subtitle = item.subtitle,
                        icon = item.icon,
                        checked = it,
                        onCheckedChanged = { newValue ->
                            scope.launch {
                                if (item.onValueChanged(newValue)) {
                                    item.pref.set(newValue)
                                }
                            }
                        },
                    )
                } ?: SwitchPreferenceWidgetPlaceholder()
            }
            is Preference.PreferenceItem.SliderPreference -> {
                val pref by item.pref.collectAsState()
                pref?.let {
                    SliderItem(
                        label = item.title,
                        min = item.min,
                        max = item.max,
                        value = it,
                        valueText = { item.subtitleProvider(it) },
                        onChange = {
                            scope.launch {
                                item.onValueChanged(it)
                            }
                        },
                    )
                }
            }
            is Preference.PreferenceItem.ListPreference<*> -> {
                val itemValue by item.pref.collectAsState()
                itemValue?.let {
                    ListPreferenceWidget(
                        value = itemValue,
                        title = item.title,
                        subtitle = item.internalSubtitleProvider(itemValue, item.entries),
                        icon = item.icon,
                        entries = item.entries,
                        onValueChange = { newValue ->
                            scope.launch {
                                if (item.internalOnValueChanged(newValue!!)) {
                                    item.internalSet(newValue)
                                }
                            }
                        },
                    )
                } ?: TextPreferencePlaceHolder(
                    item.title,
                    null,
                    item.icon
                )
            }
            is Preference.PreferenceItem.BasicListPreference -> {
                ListPreferenceWidget(
                    value = item.value,
                    title = item.title,
                    subtitle = item.subtitleProvider(item.value, item.entries),
                    icon = item.icon,
                    entries = item.entries,
                    onValueChange = { scope.launch { item.onValueChanged(it) } },
                )
            }
            is Preference.PreferenceItem.MultiSelectListPreference -> {
                val values by item.pref.collectAsState()
                values?.let {
                    MultiSelectListPreferenceWidget(
                        preference = item,
                        values = it,
                        onValuesChange = { newValues ->
                            scope.launch {
                                if (item.onValueChanged(newValues)) {
                                    item.pref.set(newValues.toMutableSet())
                                }
                            }
                        },
                    )
                }  ?: TextPreferencePlaceHolder(
                    item.title,
                    null,
                    item.icon
                )
            }
            is Preference.PreferenceItem.TextPreference -> {
                TextPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    onPreferenceClick = item.onClick,
                )
            }
            is Preference.PreferenceItem.EditTextPreference -> {
                val values by item.pref.collectAsState()
                values?.let { value ->
                    EditTextPreferenceWidget(
                        title = item.title,
                        subtitle = item.subtitle,
                        icon = item.icon,
                        value = value,
                        onConfirm = {
                            val accepted = item.onValueChanged(it)
                            if (accepted) item.pref.set(it)
                            accepted
                        },
                    )
                } ?: TextPreferencePlaceHolder(
                    item.title,
                    item.subtitle,
                    item.icon
                )
            }
            is Preference.PreferenceItem.MultiLineEditTextPreference -> {
                val values by item.pref.collectAsState()
                values?.let { value ->
                    EditTextPreferenceWidget(
                        title = item.title,
                        subtitle = item.subtitle,
                        icon = item.icon,
                        value = value,
                        onConfirm = {
                            val accepted = item.onValueChanged(it)
                            if (accepted) item.pref.set(it)
                            accepted
                        },
                        singleLine = false,
                    )
                } ?: TextPreferencePlaceHolder(
                    item.title,
                    item.subtitle,
                    item.icon
                )
            }
            is Preference.PreferenceItem.InfoPreference -> {
                InfoWidget(text = item.title)
            }
            is Preference.PreferenceItem.CustomPreference -> {
                item.content(item)
            }

            is Preference.PreferenceItem.FloatSliderPreference -> {
                val pref by item.pref.collectAsState()
                pref?.let { p ->
                    FloatSliderItem(
                        label = item.title,
                        min = item.min,
                        max = item.max,
                        value = p,
                        valueText = item.subtitleProvider,
                        steps = item.steps,
                        onChange = {
                            scope.launch {
                                item.onValueChanged(it)
                            }
                        },
                    )
                }
            }
        }
    }
}
package io.silv.movie.presentation.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.voyager.core.screen.Screen


typealias StringResource = Int

/**
 * For invoking back press to the parent activity
 */
val LocalBackPress: ProvidableCompositionLocal<(() -> Unit)?> = staticCompositionLocalOf { null }

interface SearchableSettings : Screen {

    @Composable
    @ReadOnlyComposable
    @StringRes
    fun getTitleRes(): StringResource

    @Composable
    fun getPreferences(): List<Preference>

    @Composable
    fun RowScope.AppBarAction() {
    }

    @Composable
    override fun Content() {
        val handleBack = LocalBackPress.current
        PreferenceScaffold(
            titleRes = getTitleRes(),
            onBackPressed = if (handleBack != null) handleBack::invoke else null,
            actions = { AppBarAction() },
            itemsProvider = { getPreferences() },
        )
    }

    companion object {
        // HACK: for the background blipping thingy.
        // The title of the target PreferenceItem
        // Set before showing the destination screen and reset after
        // See BasePreferenceWidget.highlightBackground
        var highlightKey: String? = null
    }
}
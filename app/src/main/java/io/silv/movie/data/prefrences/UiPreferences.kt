package io.silv.movie.data.prefrences

import android.os.Build
import androidx.annotation.StringRes
import cafe.adriel.voyager.navigator.tab.Tab
import com.google.android.material.color.DynamicColors
import io.silv.movie.R
import io.silv.movie.data.prefrences.DeviceUtil.isDynamicColorAvailable
import io.silv.movie.data.prefrences.core.PreferenceStore
import io.silv.movie.data.prefrences.core.getEnum
import io.silv.movie.presentation.browse.BrowseTab
import io.silv.movie.presentation.browse.DiscoverTab
import io.silv.movie.presentation.library.LibraryTab
import io.silv.movie.presentation.profile.ProfileTab
import io.silv.movie.presentation.settings.StringResource
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

object DeviceUtil {
    private val isSamsung: Boolean by lazy {
        Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }

    val isDynamicColorAvailable by lazy {
        (isSamsung && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) || DynamicColors.isDynamicColorAvailable()
    }
}

class UiPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun themeMode() = preferenceStore.getEnum(
        "pref_theme_mode_key",
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { ThemeMode.SYSTEM } else { ThemeMode.LIGHT },
    )

    fun appTheme() = preferenceStore.getEnum(
        "pref_app_theme",
        if (isDynamicColorAvailable) { AppTheme.MONET } else { AppTheme.DEFAULT },
    )

    fun themeDarkAmoled() = preferenceStore.getBoolean("pref_theme_dark_amoled_key", false)

    fun relativeTime() = preferenceStore.getBoolean("relative_time_v2", true)

    fun dateFormat() = preferenceStore.getString("app_date_format", "")

    fun tabletUiMode() = preferenceStore.getEnum("tablet_ui_mode", TabletUiMode.AUTOMATIC)

    fun startScreen() = preferenceStore.getEnum("start_screen", StartScreen.LIBRARY)

    companion object {
        fun dateFormat(format: String): DateFormat = when (format) {
            "" -> DateFormat.getDateInstance(DateFormat.SHORT)
            else -> SimpleDateFormat(format, Locale.getDefault())
        }
    }
}

enum class AppTheme(val titleRes: StringResource?) {
    DEFAULT(R.string.label_default),
    MONET(R.string.theme_monet),
    CLOUDFLARE(R.string.theme_cloudflare),
    COTTONCANDY(R.string.theme_cottoncandy),
    DOOM(R.string.theme_doom),
    GREEN_APPLE(R.string.theme_greenapple),
    LAVENDER(R.string.theme_lavender),
    MATRIX(R.string.theme_matrix),
    MIDNIGHT_DUSK(R.string.theme_midnightdusk),
    MOCHA(R.string.theme_mocha),
    SAPPHIRE(R.string.theme_sapphire),
    NORD(R.string.theme_nord),
    STRAWBERRY_DAIQUIRI(R.string.theme_strawberrydaiquiri),
    TAKO(R.string.theme_tako),
    TEALTURQUOISE(R.string.theme_tealturquoise),
    TIDAL_WAVE(R.string.theme_tidalwave),
    YINYANG(R.string.theme_yinyang),
    YOTSUBA(R.string.theme_yotsuba),

    // Deprecated
    DARK_BLUE(null),
    HOT_PINK(null),
    BLUE(null),
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

enum class TabletUiMode(@StringRes val titleRes: StringResource) {
    AUTOMATIC(R.string.automatic_background),
    ALWAYS(R.string.lock_always),
    LANDSCAPE(R.string.landscape),
    NEVER(R.string.lock_never),
}

enum class StartScreen(@StringRes val titleRes: StringResource, val tab: Tab) {
    LIBRARY(R.string.library_tab_title, LibraryTab),
    BROWSE(R.string.browse_tab_title, BrowseTab),
    DISCOVER(R.string.discover_tab_title, DiscoverTab),
    PROFILE(R.string.profile_tab_title, ProfileTab),
}
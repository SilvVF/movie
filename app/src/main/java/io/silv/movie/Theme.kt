package io.silv.movie

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
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
import io.silv.movie.prefrences.AppTheme
import io.silv.movie.prefrences.ThemeMode.DARK
import io.silv.movie.prefrences.ThemeMode.LIGHT
import io.silv.movie.prefrences.ThemeMode.SYSTEM
import io.silv.movie.presentation.LocalAppData

@Composable
@ReadOnlyComposable
fun getThemeColorScheme(
    appTheme: AppTheme,
    amoled: Boolean,
    dark: Boolean,
): ColorScheme {
    val colorScheme = when (appTheme) {
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
        dark,
        amoled,
    )
}

@ReadOnlyComposable
@Composable
fun isDarkTheme(): Boolean {
    val appState = LocalAppData.current
    val systemTheme = isSystemInDarkTheme()
    return when (appState.themeMode) {
        LIGHT -> false
        DARK -> true
        SYSTEM -> systemTheme
    }
}

@Composable
fun MovieTheme(
    dark: Boolean = isDarkTheme(),
    content: @Composable () -> Unit,
) {
    val appState = LocalAppData.current
    MaterialTheme(
        colorScheme = getThemeColorScheme(appState.appTheme, appState.amoled, dark),
        content = content,
    )
}
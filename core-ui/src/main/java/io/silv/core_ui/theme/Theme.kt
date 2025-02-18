package io.silv.core_ui.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.silv.core_ui.theme.dynamiccolor.DynamicScheme
import io.silv.core_ui.theme.dynamiccolor.Variant
import io.silv.core_ui.theme.hct.Hct
import io.silv.core_ui.theme.palettes.TonalPalette
import java.util.Optional
import kotlin.math.ln

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun containerColor(colorTransitionFraction: Float): Color {
    return lerp(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.applyTonalElevation(
            backgroundColor = MaterialTheme.colorScheme.surface,
            elevation = 3.0.dp,
        ),
        FastOutLinearInEasing.transform(colorTransitionFraction),
    )
}

fun ColorScheme.applyTonalElevation(
    backgroundColor: Color,
    elevation: Dp,
): Color {
    return if (backgroundColor == surface) {
        surfaceColorAtElevation(elevation)
    } else {
        backgroundColor
    }
}

@Composable
@ReadOnlyComposable
fun getColorScheme(
    darkTheme: Boolean,
    seedColor: Color? = null,
    fallback: ColorScheme? = null,
): ColorScheme {
    val context = LocalContext.current
    return when {
        seedColor != null -> {
            val palette =  TonalPalette.fromInt(seedColor.toArgb())
            DynamicScheme(
                Hct.fromInt(seedColor.toArgb()),
                Variant.NEUTRAL,
                darkTheme,
                1.0,
                palette,
                palette,
                palette,
                palette,
                palette,
                Optional.empty(),
            )
                .toColorScheme()
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> fallback ?: DarkColorScheme
        else -> fallback ?: LightColorScheme
    }
}

@Composable
fun SeededMaterialTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    seedColor: Color? = null,
    fallback: ColorScheme? = null,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = getColorScheme(darkTheme, seedColor, fallback),
        typography = Typography,
        content = content
    )
}

@SuppressLint("RestrictedApi")
fun DynamicScheme.toColorScheme() = ColorScheme(
    primary = Color(primary),
    onPrimary = Color(onPrimary),
    primaryContainer = Color(primaryContainer),
    onPrimaryContainer = Color(onPrimaryContainer),
    inversePrimary = Color(inversePrimary),
    secondary = Color(secondary),
    onSecondary = Color(onSecondary),
    secondaryContainer = Color(secondaryContainer),
    onSecondaryContainer = Color(onSecondaryContainer),
    tertiary = Color(tertiary),
    onTertiary = Color(onTertiary),
    tertiaryContainer = Color(tertiaryContainer),
    onTertiaryContainer = Color(onTertiaryContainer),
    background = Color(background),
    onBackground = Color(onBackground),
    surface = Color(surface),
    onSurface = Color(onSurface),
    surfaceVariant = Color(surfaceVariant),
    onSurfaceVariant = Color(onSurfaceVariant),
    surfaceTint = Color(primary),
    inverseSurface = Color(inverseSurface),
    inverseOnSurface = Color(inverseOnSurface),
    error = Color(error),
    onError = Color(onError),
    errorContainer = Color(errorContainer),
    onErrorContainer = Color(onErrorContainer),
    outline = Color(outline),
    outlineVariant = Color(outlineVariant),
    scrim = Color(scrim),
    Color(surfaceBright),
    Color(surfaceDim),
    Color(surfaceContainer),
    Color(surfaceContainerHigh),
    Color(surfaceContainerHighest),
    Color(surfaceContainerLow),
    Color(surfaceContainerLowest)
)

@SuppressLint("RestrictedApi")
fun DynamicScheme.surfaceColorAtElevation(
    elevation: Dp,
): Color {
    if (elevation == 0.dp) return Color(surface)
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return Color(primary).copy(alpha = alpha).compositeOver(Color(surface))
}
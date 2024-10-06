# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keep CheckedStateComponentsKt
-keep io.silv.core_ui.components.Action
-keep io.silv.core_ui.components.AnimatedEqualizerKt
-keep io.silv.core_ui.components.BadgeKt
-keep io.silv.core_ui.components.DotSeperatorTextKt
-keep io.silv.core_ui.components.EmptyScreenKt
-keep io.silv.core_ui.components.ItemCover
-keep io.silv.core_ui.components.LabledCheckBoxKt
-keep io.silv.core_ui.components.LoadingIndicatorKt
-keep io.silv.core_ui.components.PosterData
-keep io.silv.core_ui.components.PosterDataKt
-keep io.silv.core_ui.components.PullRefreshKt
-keep io.silv.core_ui.components.TmdbLogoKt
-keep io.silv.core_ui.components.TooltipIconButtonKt
-keep io.silv.core_ui.components.lazy.LazyListKt
-keep io.silv.core_ui.components.lazy.VerticalFastScrollerKt
-keep io.silv.core_ui.components.shimmer.ShimmerHostKt
-keep io.silv.core_ui.components.topbar.AppBarAction
-keep io.silv.core_ui.components.topbar.AppBarKt
-keep io.silv.core_ui.components.topbar.PosterLargeTopBarKt
-keep io.silv.core_ui.components.topbar.PosterTopBarState
-keep io.silv.core_ui.components.topbar.SearchBarInputFieldKt
-keep io.silv.core_ui.components.topbar.SearchLargeTopAppBarKt
-keep io.silv.core_ui.components.topbar.TextFieldColors2
-keep io.silv.core_ui.components.topbar.TopAppBarColors2
-keep io.silv.core_ui.theme.ThemeKt
-keep io.silv.core_ui.theme.colorScheme.BaseColorScheme
-keep io.silv.core_ui.theme.colorScheme.CloudflareColorScheme
-keep io.silv.core_ui.theme.colorScheme.CottonCandyColorScheme
-keep io.silv.core_ui.theme.colorScheme.DoomColorScheme
-keep io.silv.core_ui.theme.colorScheme.GreenAppleColorScheme
-keep io.silv.core_ui.theme.colorScheme.LavenderColorScheme
-keep io.silv.core_ui.theme.colorScheme.MatrixColorScheme
-keep io.silv.core_ui.theme.colorScheme.MidnightDuskColorScheme
-keep io.silv.core_ui.theme.colorScheme.MochaColorScheme
-keep io.silv.core_ui.theme.colorScheme.MonetColorScheme
-keep io.silv.core_ui.theme.colorScheme.NordColorScheme
-keep io.silv.core_ui.theme.colorScheme.SapphireColorScheme
-keep io.silv.core_ui.theme.colorScheme.StrawberryColorScheme
-keep io.silv.core_ui.theme.colorScheme.TachiyomiColorScheme
-keep io.silv.core_ui.theme.colorScheme.TakoColorScheme
-keep io.silv.core_ui.theme.colorScheme.TealTurqoiseColorScheme
-keep io.silv.core_ui.theme.colorScheme.TidalWaveColorScheme
-keep io.silv.core_ui.theme.colorScheme.YinYangColorScheme
-keep io.silv.core_ui.theme.colorScheme.YotsubaColorScheme
-keep io.silv.core_ui.util.AccompanistWebChromeClient
-keep io.silv.core_ui.util.AccompanistWebViewClient
-keep io.silv.core_ui.util.AccompanistWebViewKt
-keep io.silv.core_ui.util.ColorUtilKt
-keep io.silv.core_ui.util.ContextExtKt
-keep io.silv.core_ui.util.ModiferExtKt
-keep io.silv.core_ui.util.PaddingValuesKt
-keep io.silv.core_ui.util.ScrollExtKt
-keep io.silv.core_ui.util.WebViewNavigator
-keep io.silv.core_ui.util.WebViewState
-keep io.silv.core_ui.voyager.ContentScreen
-keep io.silv.core_ui.voyager.ExtKt
-keep io.silv.core_ui.voyager.ScreenResult
-keep io.silv.core_ui.voyager.ScreenResultKt
-keep io.silv.core_ui.voyager.ScreenResultsStoreProxy
-keep io.silv.core_ui.voyager.ScreenResultsViewModel
-keep io.silv.core_ui.voyager.ScreenWithResult
-keep io.silv.core_ui.voyager.ScreenWithResultLauncher
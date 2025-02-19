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

-dontwarn org.slf4j.impl.StaticLoggerBinder
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# A resource is loaded with a relative path so the package of this class must be preserved.
-keeppackagenames okhttp3.internal.publicsuffix.*
-adaptresourcefilenames okhttp3/internal/publicsuffix/PublicSuffixDatabase.gz

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

-keep class androidx.media3.session.** { *; }
# Keeps for class level annotations.

-keepclasseswithmembers,includedescriptorclasses class * {
  native <methods>;
}

# Keep ExoPlayer classes and methods
-keep class com.google.android.exoplayer2.** { *; }

# Keep classes and methods related to Media 3
-keep class com.google.android.exoplayer2.mediacodec.** { *; }
-keep class com.google.android.exoplayer2.mediacodec.surface.** { *; }

# Keep ExoPlayer-specific annotations
-keep @interface com.google.android.exoplayer2.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**
-keep class okio.** { *; }

# Converter
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

# Retrofit interfaces should be kept for Reflection
-keep,allowobfuscation interface retrofit2.Callback

-dontwarn CheckedStateComponentsKt
-dontwarn io.silv.core_ui.components.Action
-dontwarn io.silv.core_ui.components.AnimatedEqualizerKt
-dontwarn io.silv.core_ui.components.BadgeKt
-dontwarn io.silv.core_ui.components.DotSeperatorTextKt
-dontwarn io.silv.core_ui.components.EmptyScreenKt
-dontwarn io.silv.core_ui.components.ItemCover
-dontwarn io.silv.core_ui.components.LabledCheckBoxKt
-dontwarn io.silv.core_ui.components.LoadingIndicatorKt
-dontwarn io.silv.core_ui.components.PosterData
-dontwarn io.silv.core_ui.components.PosterDataKt
-dontwarn io.silv.core_ui.components.PullRefreshKt
-dontwarn io.silv.core_ui.components.TmdbLogoKt
-dontwarn io.silv.core_ui.components.TooltipIconButtonKt
-dontwarn io.silv.core_ui.components.lazy.LazyListKt
-dontwarn io.silv.core_ui.components.lazy.VerticalFastScrollerKt
-dontwarn io.silv.core_ui.components.shimmer.ShimmerHostKt
-dontwarn io.silv.core_ui.components.topbar.AppBar$Action
-dontwarn io.silv.core_ui.components.topbar.AppBarKt
-dontwarn io.silv.core_ui.components.topbar.PosterLargeTopBarKt
-dontwarn io.silv.core_ui.components.topbar.PosterTopBarState
-dontwarn io.silv.core_ui.components.topbar.SearchBarInputFieldKt
-dontwarn io.silv.core_ui.components.topbar.SearchLargeTopAppBarKt
-dontwarn io.silv.core_ui.components.topbar.TextFieldColors2
-dontwarn io.silv.core_ui.components.topbar.TopAppBarColors2
-dontwarn io.silv.core_ui.theme.ThemeKt
-dontwarn io.silv.core_ui.theme.colorScheme.BaseColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.CloudflareColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.CottonCandyColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.DoomColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.GreenAppleColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.LavenderColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.MatrixColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.MidnightDuskColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.MochaColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.MonetColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.NordColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.SapphireColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.StrawberryColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.TachiyomiColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.TakoColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.TealTurqoiseColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.TidalWaveColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.YinYangColorScheme
-dontwarn io.silv.core_ui.theme.colorScheme.YotsubaColorScheme
-dontwarn io.silv.core_ui.util.AccompanistWebChromeClient
-dontwarn io.silv.core_ui.util.AccompanistWebViewClient
-dontwarn io.silv.core_ui.util.AccompanistWebViewKt
-dontwarn io.silv.core_ui.util.ColorUtilKt
-dontwarn io.silv.core_ui.util.ContextExtKt
-dontwarn io.silv.core_ui.util.ModiferExtKt
-dontwarn io.silv.core_ui.util.PaddingValuesKt
-dontwarn io.silv.core_ui.util.ScrollExtKt
-dontwarn io.silv.core_ui.util.WebViewNavigator
-dontwarn io.silv.core_ui.util.WebViewState
-dontwarn io.silv.core_ui.voyager.ContentScreen
-dontwarn io.silv.core_ui.voyager.ExtKt
-dontwarn io.silv.core_ui.voyager.ScreenResult
-dontwarn io.silv.core_ui.voyager.ScreenResultKt
-dontwarn io.silv.core_ui.voyager.ScreenResultsStoreProxy
-dontwarn io.silv.core_ui.voyager.ScreenResultsViewModel
-dontwarn io.silv.core_ui.voyager.ScreenWithResult
-dontwarn io.silv.core_ui.voyager.ScreenWithResultLauncher

-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
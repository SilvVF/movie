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

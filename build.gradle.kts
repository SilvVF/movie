// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application").version(libs.versions.agp) apply false
    alias(libs.plugins.kotlin.serializtion).version(libs.versions.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.com.android.library) apply false
    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.movie.android.library.compose) apply false
    alias(libs.plugins.movie.android.application.compose) apply false
    alias(libs.plugins.movie.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.24" apply false
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.1" apply false
    alias(libs.plugins.kotlin.serializtion).version(libs.versions.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.com.android.library) apply false
    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.movie.android.library.compose) apply false
    alias(libs.plugins.movie.android.application.compose) apply false
    alias(libs.plugins.movie.android.application) apply false
}
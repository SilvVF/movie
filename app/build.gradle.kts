import java.util.Properties

plugins {
    alias(libs.plugins.movie.android.application.compose)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.movie.android.application)
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("app.cash.sqldelight") version "2.0.1"
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("io.silv")
        }
    }
}

android {
    namespace = "io.silv.movie"

    defaultConfig {
        applicationId = "io.silv.movie"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    val properties = Properties().also {
        it.load(project.rootProject.file("local.properties").inputStream())
    }

    buildTypes.onEach { buildType ->
        buildType.buildConfigField("String", "TMDB_API_KEY", properties.getProperty("TMDB_API_KEY"))
        buildType.buildConfigField("String", "TMDB_ACCESS_TOKEN", properties.getProperty("TMDB_ACCESS_TOKEN"))
        buildType.buildConfigField("String", "SUPABASE_URL", properties.getProperty("SUPABASE_URL"))
        buildType.buildConfigField("String", "SUPABSE_ANON_KEY", properties.getProperty("SUPABSE_ANON_KEY"))
        buildType.buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", properties.getProperty("GOOGLE_WEB_CLIENT_ID"))
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
dependencies {

    implementation(project(":core-ui"))
    implementation(libs.androidx.activity)

    // COMPOSE
    implementation(libs.androidx.animation.graphics.android)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.compose.foundation)
    androidTestImplementation(libs.androidx.compose.ui.test.junit)
    debugImplementation(libs.androidx.compose.ui.manifest)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.compose.ui.animation.core)
    implementation(libs.androidx.compose.ui.animation)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.animation)

    implementation(libs.androidx.core.splashscreen)

    implementation("org.jsoup:jsoup:1.18.1")
    implementation(libs.okhttp.dnsoverhttps)
    implementation("com.github.teamnewpipe:NewPipeExtractor:0.24.4")

    // Paging3
    implementation(libs.bundles.paging)

    // Compose Libs
    implementation(libs.reorderable)
    implementation(libs.haze)
    implementation(libs.font.awesome)
    implementation("com.github.jeziellago:compose-markdown:0.5.0")

    // COIL
    implementation(libs.coil.compose)
    implementation(libs.coil)
    implementation(libs.coil.gif)
    implementation(libs.zoomable.image.coil)

    // Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.datasource.okhttp)

    // Datastore
    implementation(libs.androidx.datastore.preferences)

    // Logging
    implementation(libs.timber)

    // Network
    implementation(libs.okio)
    implementation(libs.retrofit.kotlinx.serializtion.converter)
    implementation(libs.retrofit)
    implementation(libs.okhttp)

    // VOYAGER
    implementation(libs.bundles.voyager)

    // KOIN
    implementation(libs.bundles.koin)
    implementation(libs.koin.workmanager)

    // KOTLIN
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json.okio)

    // Database
    implementation(libs.stately.concurrency)
    implementation(libs.bundles.sqldelight)

    // Work Manager
    implementation(libs.androidx.work.runtime.ktx)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.ktor.client.okhttp)
    implementation(libs.bundles.supabase)
}

tasks {
    // See https://kotlinlang.org/docs/reference/experimental.html#experimental-status-of-experimental-api(-markers)
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-opt-in=coil.annotation.ExperimentalCoilApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}
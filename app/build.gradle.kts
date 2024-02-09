import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
}

android {
    namespace = "io.silv.movie"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.silv.movie"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}


dependencies {

    implementation(project(":core"))
    implementation(project(":core-ui"))
    implementation(project(":data"))
    implementation(project(":core-network"))

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    // COMPOSE
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.voyager.screenModel)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.foundation)
    androidTestImplementation(libs.androidx.compose.ui.test.junit)
    androidTestImplementation(composeBom)
    debugImplementation(libs.androidx.compose.ui.manifest)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.graphics)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.material3.android)

    implementation(libs.androidx.compose.ui.animation.core)
    implementation(libs.androidx.compose.ui.animation)

    implementation(libs.haze)

    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.common)

    implementation(libs.androidx.compose.ui.util)

    // Datastore
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.okio)

    // VOYAGER
    implementation(libs.voyager.koin)
    implementation(libs.voyager.navigator)
    implementation(libs.voyager.transitions)
    implementation(libs.voyager.tabNavigator)

    // KOIN
    implementation(libs.koin.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.koin.navigation)
    implementation(libs.koin.workmanager)

    // COIL
    implementation(libs.coil.compose)
    implementation(libs.coil)

    // KOTLIN
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlin.collections.immutable)
}

tasks {
    // See https://kotlinlang.org/docs/reference/experimental.html#experimental-status-of-experimental-api(-markers)
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
            "-opt-in=coil.annotation.ExperimentalCoilApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }
}
@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.movie.android.library.compose)
    alias(libs.plugins.org.jetbrains.kotlin.android)
}

android {
    namespace = "io.silv.core_ui"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.material3.android)
    androidTestImplementation(libs.androidx.compose.ui.test.junit)
    debugImplementation(libs.androidx.compose.ui.manifest)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui.animation.core)
    implementation(libs.androidx.compose.ui.animation)

    implementation(libs.androidx.compose.ui.util)

    implementation(libs.compose.shimmer)

    api("com.google.android.material:material:1.12.0")

    // COIL
    implementation(libs.coil.compose)
    implementation(libs.coil)

    implementation(libs.androidx.palette)

    implementation(libs.kotlin.collections.immutable)
    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.paging.compose)

    implementation(libs.bundles.voyager)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)

}

tasks {
    // See https://kotlinlang.org/docs/reference/experimental.html#experimental-status-of-experimental-api(-markers)
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-opt-in=coil.annotation.ExperimentalCoilApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }
}
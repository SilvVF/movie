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
            isMinifyEnabled = false
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

    kotlin {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-opt-in=coil.annotation.ExperimentalCoilApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.material3.android)
    debugImplementation(libs.androidx.compose.ui.manifest)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // COIL
    api(libs.coil.compose)
    api(libs.compose.shimmer)
    api(libs.androidx.palette)

    api(libs.androidx.paging.compose)

    api(libs.voyager.screenModel)

    implementation(libs.androidx.core.ktx)
}

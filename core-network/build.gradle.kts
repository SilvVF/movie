import java.util.Properties

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    kotlin("plugin.serialization")
}

android {
    namespace = "io.silv.core_network"
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

    val properties = Properties().also {
        it.load(project.rootProject.file("local.properties").inputStream())
    }


    buildTypes.onEach { buildType ->
        buildType.buildConfigField("String", "TMDB_API_KEY", properties.getProperty("TMDB_API_KEY"))
        buildType.buildConfigField("String", "TMDB_ACCESS_TOKEN", properties.getProperty("TMDB_ACCESS_TOKEN"))
    }
    buildFeatures {
        buildConfig = true
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

    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)

    implementation(libs.retrofit.kotlinx.serializtion.converter)
    implementation(libs.retrofit)

    implementation(libs.koin.core)
    implementation(libs.koin.android)

    implementation(libs.okio)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.okhttp)
    implementation(libs.okhttp.dnsoverhttps)

    implementation(libs.retrofit)

    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.paging.runtime)

    implementation(libs.kotlinx.serialization.json.okio)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.datetime)
}
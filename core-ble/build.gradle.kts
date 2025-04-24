@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.library)      // com.android.library
    alias(libs.plugins.kotlin.android)       // org.jetbrains.kotlin.android
    alias(libs.plugins.hilt.gradle)         // dagger.hilt.android.plugin
    alias(libs.plugins.kotlin.kapt)          // for Hilt codegen
}

android {
    namespace   = "com.example.disastermesh.core.ble"
    compileSdk  = 35

    defaultConfig {
        minSdk                    = 23
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "com.example.disastermesh.core.testing.HiltTestRunner"
    }

    buildFeatures {
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Hilt for DI
    implementation(libs.hilt.android)
    kapt         (libs.hilt.compiler)

    // Coroutines for any Flow/async work in your scanner
    implementation(libs.kotlinx.coroutines.android)

    // (Optional) If you wrap Android’s BLE APIs in wrapper libraries:
    // implementation(libs.androidx.bluetooth)
}

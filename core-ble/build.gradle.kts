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
        minSdk                    = 24
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
    implementation(project(":core-database"))
    implementation(project(":core-database"))
    implementation(project(":core-data"))
    // Hilt for DI
    implementation(libs.hilt.android)
    implementation(libs.androidx.room.common.jvm)
    kapt         (libs.hilt.compiler)

    // Coroutines for any Flow/async work in your scanner
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    //TODO: Issue in preferences 1.1.5 use 1.1.4 for now then bring up later
    implementation("androidx.datastore:datastore-preferences:1.1.4")

}

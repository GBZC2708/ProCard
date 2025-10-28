plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.procard"
    compileSdk = 36 // Usa la que proponga tu asistente; 35/36 es común en 2025


    defaultConfig {
        applicationId = "com.example.procard"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation("androidx.navigation:navigation-compose:2.8.3")

    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("com.google.android.material:material:1.12.0")
// Material Components (M3)
    implementation("androidx.appcompat:appcompat:1.7.0")
// si usas AppCompatActivitydependencies {
    // ... other dependencies
    implementation("com.google.android.material:material:1.11.0")
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-saveable")
    implementation("androidx.navigation:navigation-compose")
}


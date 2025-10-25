plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}


android {
    namespace = "com.example.procard"
    compileSdk = 35 // Usa la que proponga tu asistente; 35/36 es común en 2025


    defaultConfig {
        applicationId = "com.example.procard"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "latest" }
    kotlinOptions { jvmTarget = "17" }
}


dependencies {
// Compose BOM (gestiona versiones coherentes)
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))


    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")


// Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.3")


// Coil para avatar
    implementation("io.coil-kt:coil-compose:2.7.0")
}
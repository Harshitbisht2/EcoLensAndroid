plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.knight.ecolens"
    compileSdk = 34 // High enough for 2026 standards

    defaultConfig {
        applicationId = "com.knight.ecolens"
        minSdk = 26     // Supports ~90% of devices, allows better java features.
        targetSdk = 34  // Required for Google Play Store uploads in 2025/26
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        // Essential for using modern Java features like Records or Lambdas.
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    //CameraX (Essential for the "Eyes" of EcoLens)
    val cameraVersion = "1.4.0"
    implementation("androidx.camera:camera-core:$cameraVersion")
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("android.camera:camera-view:$cameraVersion")

    // ML kit (The "Brain" of EcoLens)
    implementation("com.google.mlkit:object-detection:17.0.2")

    // Material Design (for the UI)
    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
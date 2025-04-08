plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.safeharbor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.safeharbor"
        minSdk = 21
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Location services
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // ✅ Maps Utils (for GeoJSON, polygon parsing, etc.)
    implementation("com.google.maps.android:android-maps-utils:2.2.5")
}


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // Plugin de Google Services (correcto aquí)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.onfu.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.onfu.app"
        minSdk = 26
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {
    // Android & Compose
    implementation(libs.androidx.core.ktx)
    // AppCompat (needed for AppCompatActivity, fragments, and traditional views)
    implementation("androidx.appcompat:appcompat:1.6.1")
    // Fragment KTX for fragment transactions and utilities
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    // ConstraintLayout for XML ConstraintLayout attributes
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.navigation:navigation-compose:2.8.0-beta05")
    implementation("io.coil-kt:coil-compose:2.4.0")


    // BoM de Firebase (solo una vez)
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))

    // Firebase (sin versiones porque usamos BOM)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
    // Google Sign-In (para obtener idToken y correo)
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)


}

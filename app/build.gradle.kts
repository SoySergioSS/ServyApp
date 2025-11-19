import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.dagger)
    kotlin("kapt")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.example.servyapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.servyapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }

        val geminiApiKey = properties.getProperty("GEMINI_API_KEY") ?: ""

        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Room Database
    // implementation(libs.androidx.room.ktx) // Ya está o se puede agregar
    implementation("androidx.room:room-runtime:2.8.0")
    ksp("androidx.room:room-compiler:2.8.0")
    // Gson para convertir objetos a JSON en Room
    implementation("com.google.code.gson:gson:2.10.1")

    //hilt
    implementation(libs.dagger.hilt)
    implementation(libs.hilt.compose.navigation)
    kapt(libs.dagger.kapt)

    //firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    //Navegacion
    implementation("androidx.navigation:navigation-compose:2.7.2")
    implementation("androidx.compose.material:material-icons-extended:$2024.04.01")

    implementation("androidx.compose.runtime:runtime-livedata:1.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    implementation("io.coil-kt:coil-compose:2.4.0")

    //Librería de escaneo QR
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    //vico para graficos de barra
    implementation(libs.vico.compose.m3)

    //Pie Charts
    implementation("co.yml:ycharts:2.1.0")

    // SDK de Google AI para Android (Gemini)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
}
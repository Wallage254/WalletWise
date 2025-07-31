plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.wallet_wise"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.wallet_wise"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX Core libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Network libraries - Choose either Volley OR Retrofit (recommended: Retrofit)
    implementation(libs.volley) // Keep if you need Volley for other parts

    // Retrofit for REST API calls (recommended for structured API calls)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation (libs.glide)
    implementation(libs.gridlayout)
    annotationProcessor (libs.compiler)
    implementation(libs.mpandroidchart)
    implementation(libs.text.recognition)
    implementation(libs.exp4j)
    implementation(libs.work.runtime.ktx)


    // Material Design (you already have this in libs.material, but explicit version if needed)
    implementation(libs.material.v1110)
    implementation(libs.firebase.auth)
    //noinspection UseTomlInstead
    implementation ("com.google.android.gms:play-services-auth:21.4.0")
    implementation(libs.firebase.database)



    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.firebase)
    alias(libs.plugins.hilt)
    alias(libs.plugins.crashanlytics)
    kotlin("kapt")
}

/**
 * The secrets that needs to be added to BuildConfig at runtime.
 */
val secrets = arrayOf(
    "IAP_BUY_ME_COFFEE",
    "IAP_NO_ADS",
    "PLACEMENT_BANNER_1",
    "PLACEMENT_BANNER_2",
    "PLACEMENT_INTERSTITIAL",
    "UNITY_APP_ID",
    "PLAY_CONSOLE_APP_RSA_KEY",
    "IAP_TAG_EDITOR_PRO"
)

android {
    compileSdk = 34
    namespace = "com.prime.media"
    defaultConfig {
        applicationId = "com.prime.player"
        minSdk = 27
        targetSdk = 34
        versionCode = 57
        versionName = "2.6.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        //Load secrets into BuildConfig
        secrets.forEach { secret ->
            val value = "\"" + (System.getenv(secret) ?: "no_value") + "\""
            buildConfigField("String", secret, value)
        }
    }

    buildTypes {
        // Make sure release is version is optimised.
        release {
            isMinifyEnabled = true
            isZipAlignEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        // Add necessary changes to debug apk.
        debug {
            // makes it possible to install both release and debug versions in same device.
            applicationIdSuffix = ".debug"
            resValue("string", "app_name2", "Debug")
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xcontext-receivers")
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.3" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

// Not moving these to libs.version.toml because i think this is redundant.
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    val compose_version = "1.6.0-alpha02"
    implementation("androidx.compose.ui:ui:$compose_version")
    implementation("androidx.compose.ui:ui-tooling-preview:$compose_version")
    implementation("androidx.compose.animation:animation-graphics:$compose_version")
    implementation("androidx.compose.material:material:$compose_version")
    // Integration with activities
    implementation("androidx.activity:activity-compose:1.7.2")
    // Material design icons
    implementation("androidx.compose.material:material-icons-core:$compose_version")
    implementation("androidx.compose.material:material-icons-extended:$compose_version")
    // The Accompanist Libraries
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    //Lottie
    implementation("com.airbnb.android:lottie-compose:6.1.0")
    // Preferences and other widgets
    val toolkit_version = "1.1.1"
    implementation("com.github.prime-zs.toolkit:preferences:$toolkit_version")
    implementation("com.github.prime-zs.toolkit:core-ktx:$toolkit_version")
    implementation("com.github.prime-zs.toolkit:material2:$toolkit_version")
    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.2.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    // WindowSizeClasses
    implementation("androidx.compose.material3:material3-window-size-class:1.1.1")
    // Google Play InAppUpdate
    val in_app_update_version = "2.1.0"
    implementation("com.google.android.play:app-update:$in_app_update_version")
    implementation("com.google.android.play:app-update-ktx:$in_app_update_version")
    // Google Play InAppReview
    val in_app_review = "2.0.1"
    implementation("com.google.android.play:review:$in_app_review")
    implementation("com.google.android.play:review-ktx:$in_app_review")
    // Google play in-app billing
    val billing_version = "6.0.1"
    implementation("com.android.billingclient:billing:$billing_version")
    implementation("com.android.billingclient:billing-ktx:$billing_version")
    // Unity Ads
    implementation("com.unity3d.ads:unity-ads:4.8.0")
    // Compose navigation
    implementation("androidx.navigation:navigation-compose:2.7.2")
    // Compose Downloadable fonts
    implementation("androidx.compose.ui:ui-text-google-fonts:1.5.1")
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
    // Room Database
    val room_version = "2.6.0-beta01"
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    //Ken Burns
    // FixMe: Build Compose alternative; check compose marque modifier.
    implementation("com.flaviofaria:kenburnsview:1.0.7")
    //Wavy Slider
    implementation("ir.mahozad.multiplatform:wavy-slider:0.0.1")
    // Constraint Layout
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    val media3_version = "1.1.1"
    // For media playback using ExoPlayer
    implementation("androidx.media3:media3-exoplayer:$media3_version")
    // For exposing and controlling media sessions
    implementation("androidx.media3:media3-session:$media3_version")
    //Tag Editor
    // Currently it only supports mp3;
    implementation("com.mpatric:mp3agic:0.9.1")
}

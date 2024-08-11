plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.firebase)
    alias(libs.plugins.hilt)
    alias(libs.plugins.crashanlytics)
    kotlin("kapt")
    alias(libs.plugins.compose.compiler)
}

/**
 * The secrets that needs to be added to BuildConfig at runtime.
 */
val secrets = arrayOf(
    "IAP_BUY_ME_COFFEE",
    "IAP_NO_ADS",
    "ADS_APP_ID",
    "PLAY_CONSOLE_APP_RSA_KEY",
    "IAP_TAG_EDITOR_PRO",
    "IAP_CODEX"
)

android {
    compileSdk = 34
    namespace = "com.prime.media"
    defaultConfig {
        applicationId = "com.prime.player"
        minSdk = 21
        targetSdk = 34
        versionCode = 116
        versionName = "2.15.0-beta02"
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
        freeCompilerArgs = listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xcontext-receivers",
            "-Xopt-in=com.primex.core.ExperimentalToolkitApi"
        )
    }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    dynamicFeatures += setOf(":app:codex")
    composeCompiler { enableStrongSkippingMode = false }
}

// Not moving these to libs.version.toml because i think this is redundant.
dependencies {
    implementation(libs.compose.activity)

    implementation(libs.bundles.compose)
    implementation(libs.bundles.preview)
    implementation(libs.bundles.icons)
    implementation(libs.bundles.analytics)
    implementation(libs.bundles.play.services)
    implementation(libs.bundles.media3)
    implementation(libs.bundles.room)
    implementation(libs.bundles.hilt)

    implementation(libs.coil)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.adaptive)
    implementation(libs.lottie)
    implementation(libs.toolkit.preferences)
    implementation(libs.splashscreen)
    implementation(libs.window)

    implementation(libs.navigation.compose)
    implementation(libs.ui.text.google.fonts)
    implementation(libs.core.ktx)

    kapt(libs.hilt.android.compiler)
    kapt(libs.room.compiler)

    implementation(libs.kenburnsview)
    implementation(libs.wavy.slider)
    implementation(libs.constraint.layout)
    implementation(libs.androidx.palette.ktx)

    // TODO - Consider using this as Dynamic Feature Module in future.
    implementation(project(":app:ads"))
    //TODO - Updating dependencies caused the app not to compile because of some issue with
    //     internal below dependency and hence this. Remove this in next update.
    implementation("com.google.j2objc:j2objc-annotations:3.0.0")
}

// TODO: It appears that Material3 components may be leaking into this project, which is intended to support Material2.
//       Please investigate if this issue is related to the Wavy Slider and resolve it. Once the main issue is fixed,
//       consider removing this block of code.
configurations {
    all { exclude(group = "androidx.compose.material3", module = "material3") }
}

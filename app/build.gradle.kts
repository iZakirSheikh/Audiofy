import com.android.build.api.dsl.ApplicationDefaultConfig

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.service)
    alias(libs.plugins.crashanlytics)
}

// The secrets that needs to be added to BuildConfig at runtime.
private val secrets = arrayOf("ADS_APP_ID", "PLAY_CONSOLE_APP_RSA_KEY")

/**
 * Adds a string BuildConfig field to the project.
 */
private fun ApplicationDefaultConfig.buildConfigField(name: String, value: String) =
    buildConfigField("String", name, "\"" + value + "\"")

android {
    //
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    // Android SDK version
    compileSdk = 35
    namespace = "com.prime.media"
    // Java version
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // App version 4.0.0
    // Version 4.0.0 will start from version code 1000
    defaultConfig {
        applicationId = "com.prime.player"
        minSdk = 21
        targetSdk = 35
        versionCode = 1000
        versionName = "4.0.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        // Load secrets into BuildConfig
        // These are passed through env of github.
        for (secret in secrets) {
            buildConfigField(secret, System.getenv(secret) ?: "")
        }
    }
    // App build types
    buildTypes {
        // Configuration for the release build type.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }

        // Configuration for the debug build type.
        // Appends ".debug" to the application ID. This allows installing debug and release versions on the same device.
        // Defines a string resource specifically for the debug build.
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "launcher_label", "Debug")
            versionNameSuffix = "-debug"
        }
    }
    // Enable experimental feature here of Kotlin Plugin
    kotlinOptions {
        jvmTarget = "11"
        //
        freeCompilerArgs = listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xwhen-guards",
            "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-Xopt-in=com.zs.compose.theme.ExperimentalThemeApi",
            "-Xnon-local-break-continue"
        )
    }
    dynamicFeatures += setOf(":telemetry")
    // dynamicFeatures += setOf(":telemetry")
    // Config. the compose compiler
    composeCompiler {
        // enableStrongSkippingMode = false
        // TODO - I guess disable these in release builds.reportsDestination =
        //     layout.buildDirectory.dir("compose_compiler")
        // metricsDestination = layout.buildDirectory.dir("compose_compiler")
        // stabilityConfigurationFiles = listOf(
        //     rootProject.layout.projectDirectory.file("stability_config.conf")
        // )
    }
}

// Define the app dependencies.
dependencies {
    implementation(libs.androidx.koin)
    implementation(libs.toolkit.preferences)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.chrisbanes.haze)
    implementation(libs.navigation.compose)
    implementation(libs.lottie.compose)
    implementation(libs.toolkit.theme)
    implementation(libs.toolkit.foundation)
    // Play
    implementation(libs.play.app.update.ktx)
    implementation(libs.play.app.review.ktx)

    // bundles
    implementation(libs.bundles.icons)
    implementation(libs.bundles.compose.ui)
    debugImplementation(libs.bundles.compose.ui.tooling)
    api(project(":core"))
}
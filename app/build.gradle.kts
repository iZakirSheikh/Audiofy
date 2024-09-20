import com.android.build.api.dsl.ApplicationDefaultConfig

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
    "ADS_APP_ID",
    "PLAY_CONSOLE_APP_RSA_KEY",
)

/**
 * Adds a string BuildConfig field to the project.
 */
private fun ApplicationDefaultConfig.buildConfigField(name: String, value: String) =
    buildConfigField("String", name, "\"" + value + "\"")

/**
 * Init
 */
private fun ApplicationDefaultConfig.init() {
    // Load secrets into BuildConfig
    // These are passed through env of github.
    secrets.forEach { secret ->
        buildConfigField(secret, System.getenv(secret) ?: "")
    }

    // Load InAppProduct names into BuildConfig for easy access. No security risk in exposing these.
    // Offer both individual and "full group" purchases (latter incentivizes more revenue and user savings).
    // Buying "full group" unlocks all items within that group.
    buildConfigField("IAP_TAG_EDITOR_PRO", "tag_editor_pro")
    buildConfigField("IAP_BUY_ME_COFFEE", "buy_me_a_coffee")
    buildConfigField("IAP_CODEX", "buy_codex")
    buildConfigField("IAP_NO_ADS", "disable_ads")

    // Widgets inspired by Android notifications, organized into groups.
    // Purchasing a group unlocks all current widgets within it.
    // Bonus: Any future widgets added to that group will also be unlocked automatically!
    // A widget name can be anything; it might represent where from the widget got inspired.
    //
    // Available widget within group IAP_WIDGETS_PLATFORM:
    // - IAP_PLATFORM_WIDGET_IPHONE: Inspired by the notification design on iPhone 15.
    // - IAP_PLATFORM_WIDGET_SNOW_CONE: Inspired by the weather app widget on my phone.
    // - IAP_PLATFORM_WIDGET_TIRAMISU: Inspired by Android 13 notification design.
    // - IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE: Inspired by Android 11 notification design.
    buildConfigField("IAP_WIDGETS_PLATFORM", "widgets_platform") // group
    buildConfigField("IAP_PLATFORM_WIDGET_IPHONE", "platform_widget_iphone")
    buildConfigField("IAP_PLATFORM_WIDGET_TIRAMISU", "platform_widget_tiramisu")
    buildConfigField("IAP_PLATFORM_WIDGET_SNOW_CONE", "platform_widget_snow_cone")
    buildConfigField("IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE", "platform_widget_red_violet_cake")
    // ColorCroft Widget Bundle
    buildConfigField("IAP_COLOR_CROFT_WIDGET_BUNDLE", "aurora_widget_bundle")
    buildConfigField("IAP_COLOR_CROFT_GRADIENT_GROVES", "color_craft_gradient_groves")
    buildConfigField("IAP_COLOR_CROFT_GOLDEN_DUST", "color_craft_golden_dust")
    // Group
}

android {
    compileSdk = 34
    namespace = "com.prime.media"
    defaultConfig {
        applicationId = "com.prime.player"
        minSdk = 21
        targetSdk = 35
        versionCode = 135
        versionName = "2.17.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        // init different config fields.
        init()
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
           // applicationIdSuffix = ".debug"
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
    // The preview is not required in release builds.
    debugImplementation(libs.bundles.preview)
    implementation(libs.androidx.graphics.shapes)
}

// TODO: It appears that Material3 components may be leaking into this project, which is intended to support Material2.
//       Please investigate if this issue is related to the Wavy Slider and resolve it. Once the main issue is fixed,
//       consider removing this block of code.
configurations {
    all { exclude(group = "androidx.compose.material3", module = "material3") }
}

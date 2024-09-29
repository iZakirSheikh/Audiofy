plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.zs.core_ui"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=com.primex.core.ExperimentalToolkitApi"
        )
    }
    buildFeatures { compose = true }
}

dependencies {
    api(libs.bundles.compose)
    api(libs.bundles.material.icons)
    debugApi(libs.bundles.compose.preview)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.window)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.graphics.shapes)
    // TODO - Revert these to impl once old code is removed from the project.
    api(libs.wavy.slider)
    api(libs.lottie.compose)
}

// TODO: It appears that Material3 components may be leaking into this project, which is intended to support Material2.
//       Please investigate if this issue is related to the Wavy Slider and resolve it. Once the main issue is fixed,
//       consider removing this block of code.
configurations {
    all { exclude(group = "androidx.compose.material3", module = "material3") }
}
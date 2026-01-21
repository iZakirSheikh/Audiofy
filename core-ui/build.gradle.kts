import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
}

//
kotlin {
    compilerOptions {
        // Target JVM bytecode version (was "11" string, now typed enum)
        jvmTarget = JvmTarget.JVM_17

        // Add experimental/advanced compiler flags
        freeCompilerArgs.addAll(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=com.primex.core.ExperimentalToolkitApi"
        )
    }
}

android {
    namespace = "com.zs.core_ui"
    compileSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

dependencies {
    api(libs.bundles.compose)
    api(libs.bundles.material.icons)
    api(libs.coil.compose)
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
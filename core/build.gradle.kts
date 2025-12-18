import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
}

//
kotlin {
    compilerOptions {
        // Target JVM bytecode version (was "11" string, now typed enum)
        jvmTarget = JvmTarget.JVM_17
    }
}

android {
    namespace = "com.zs.core"
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.room)
    implementation(libs.google.billing.ktx)

    ksp(libs.room.compiler)
    api(libs.bundles.media3)
    api(libs.mp3agic)
}
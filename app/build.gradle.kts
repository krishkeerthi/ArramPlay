import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    kotlin("plugin.serialization") version "1.6.0"
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(key: String): String = localProps.getProperty(key, "")

android {
    namespace = "com.krish.videoplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.krish.videoplayer"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "RTMP_HOST",             "\"${secret("RTMP_HOST")}\"")
        buildConfigField("String", "RTMP_DOMAIN",           "\"${secret("RTMP_DOMAIN")}\"")
        buildConfigField("String", "RTMP_PATH_ARRAM",       "\"${secret("RTMP_PATH_ARRAM")}\"")
        buildConfigField("String", "RTMP_PATH_RR",          "\"${secret("RTMP_PATH_RR")}\"")
        buildConfigField("String", "RTMP_PATH_LIVEUPDATES", "\"${secret("RTMP_PATH_LIVEUPDATES")}\"")
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

    flavorDimensions += "formFactor"
    productFlavors {
        create("mobile") {
            dimension = "formFactor"
            // Keeps the existing applicationId so the live Play listing is unaffected.
        }
        create("tv") {
            dimension = "formFactor"
            applicationIdSuffix = ".tv"
            versionNameSuffix = "-tv"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // Required for 16 KB page size support on Android 15+ devices.
        // AGP 8.5.1+ with useLegacyPackaging = false aligns native libs to 16 KB
        // and stores them uncompressed inside the APK so the loader can mmap them.
        jniLibs {
            useLegacyPackaging = false
            // io.antmedia:rtmp-client:3.2.0 (transitive via media3-datasource-rtmp)
            // ships a 4 KB-aligned librtmp-jni.so. We compile our own 16 KB-aligned
            // version under src/main/jniLibs and pickFirst makes ours win the
            // duplicate-resolution race with the AAR-bundled copy.
            pickFirsts += "lib/arm64-v8a/librtmp-jni.so"
            pickFirsts += "lib/armeabi-v7a/librtmp-jni.so"
            pickFirsts += "lib/x86/librtmp-jni.so"
            pickFirsts += "lib/x86_64/librtmp-jni.so"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.rtsp)
    implementation(libs.androidx.media3.datasource.rtmp)
    implementation(libs.androidx.media3.ui)
    implementation(libs.media3.exoplayer)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization)
    implementation(libs.androidx.tools.core)
    implementation(libs.android.play.integrity)
    implementation(libs.androidx.leanback)
    implementation(libs.app.update.ktx)
//    implementation(libs.androidx.wear)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(kotlin("script-runtime"))
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
}
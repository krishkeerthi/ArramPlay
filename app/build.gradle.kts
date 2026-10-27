import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(key: String): String = localProps.getProperty(key, "")

android {
    namespace = "com.krish.videoplayer"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.krish.videoplayer"
        minSdk = 24
        targetSdk = 36
        versionCode = 12
        versionName = "2.7"

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
        debug {
            // Lets a debug build sit next to the Play Store install on the same device.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
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
        // Path B: both flavors share applicationId "com.krish.videoplayer" so they
        // upload to the same Play Console listing. Uniqueness within the listing is
        // achieved by giving the tv flavor a non-overlapping versionCode range.
        create("mobile") {
            dimension = "formFactor"
            // versionCode inherited from defaultConfig (currently 12).
        }
        create("tv") {
            dimension = "formFactor"
            // Offset keeps tv builds permanently distinct from mobile —
            // bump defaultConfig.versionCode for mobile, this expression auto-tracks.
            // Bump this offset to release a tv-only build without touching mobile.
            versionCode = (defaultConfig.versionCode ?: 1) + 10_001
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
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

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.rtsp)
    implementation(libs.androidx.media3.datasource.rtmp)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization)
    implementation(libs.app.update.ktx)
    implementation(libs.androidx.fragment)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

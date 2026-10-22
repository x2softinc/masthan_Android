import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// VERSION_API_KEY mirrors the iOS Info.plist key. Lookup order: local.properties (git-ignored),
// ~/.gradle/gradle.properties, then the VERSION_API_KEY environment variable.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val versionApiKey: String = localProps.getProperty("VERSION_API_KEY")
    ?: providers.gradleProperty("VERSION_API_KEY").orNull
    ?: System.getenv("VERSION_API_KEY")
    ?: ""

android {
    namespace = "com.x2softinc.masthan"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.x2softinc.masthan"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "2.0"

        buildConfigField("String", "VERSION_API_KEY", "\"$versionApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileSdkMinor = 0
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}

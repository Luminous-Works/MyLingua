import java.util.Properties

plugins {
    id("com.google.gms.google-services")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/** Reads a git-ignored .properties file from the project root, or returns an empty set. */
fun rootProperties(name: String): Properties = Properties().apply {
    val file = rootProject.file(name)
    if (file.exists()) file.inputStream().use { load(it) }
}

// reCAPTCHA Enterprise site key for App Check in release builds.
// Set `recaptcha.siteKey=...` in local.properties (git-ignored), or the RECAPTCHA_SITE_KEY
// environment variable. Never commit it: the contest rules forbid keys in the repository.
val recaptchaSiteKey: String = rootProperties("local.properties").getProperty("recaptcha.siteKey")
    ?: System.getenv("RECAPTCHA_SITE_KEY")
    ?: ""

// Release signing. keystore.properties (git-ignored) holds storeFile, storePassword,
// keyAlias and keyPassword. Without it, release builds are produced unsigned.
val keystoreProperties = rootProperties("keystore.properties")

android {
    namespace = "com.luminousworksllc.mylingual"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.luminousworksllc.mylingual"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "1.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "RECAPTCHA_SITE_KEY", "\"$recaptchaSiteKey\"")
    }

    signingConfigs {
        if (!keystoreProperties.isEmpty) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Import the Firebase BoM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:35.2.0"))

    // Gemini via Firebase AI Logic
    implementation("com.google.firebase:firebase-ai")
    // Hybrid (on-device Gemini Nano) support. Experimental, and not yet in the Firebase BoM.
    implementation("com.google.firebase:firebase-ai-ondevice:16.0.0-beta08")

    // App Check. Release builds attest with reCAPTCHA Enterprise (src/release).
    // The debug provider is debug-only (src/debug), so it never ships in a release APK.
    implementation("com.google.firebase:firebase-appcheck-recaptcha")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ViewModel and lifecycle-aware state collection for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Public client configuration only. Never put a service-role key in an APK.
val supabaseProperties = Properties().apply {
    val config = rootProject.file("supabase.properties")
    if (config.exists()) config.inputStream().use { load(it) }
}
val supabaseUrl = supabaseProperties.getProperty("SUPABASE_URL", "").trim().trimEnd('/')
val supabaseKey = supabaseProperties.getProperty("SUPABASE_PUBLISHABLE_KEY", "").trim()
require(supabaseUrl.isEmpty() || Regex("https://[a-z0-9-]+\\.supabase\\.co").matches(supabaseUrl)) {
    "SUPABASE_URL must be an HTTPS project API URL, not a dashboard URL."
}
require(supabaseKey.isEmpty() || Regex("sb_publishable_[A-Za-z0-9_-]+").matches(supabaseKey)) {
    "Only a Supabase publishable key is supported. Never use a secret or service-role key."
}
require(supabaseUrl.isEmpty() == supabaseKey.isEmpty()) {
    "Set both SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY, or leave both empty for a local-only build."
}

android {
    namespace = "com.example.routineapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.routineapp"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"$supabaseKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.health.connect.client)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

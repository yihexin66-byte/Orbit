plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Backend base URL is build-time configurable so the same sources can target
// the uniCloud URL化 endpoint, a local tunnel or 10.0.2.2:
//   ./gradlew assembleDebug -PorbitApiBaseUrl=https://<space-id>/orbit/
val orbitApiBaseUrlValue: String = providers.gradleProperty("orbitApiBaseUrl")
    .orElse("https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit/")
    .get()
    .let { if (it.endsWith("/")) it else "$it/" }
val orbitApiBaseUrl: String = orbitApiBaseUrlValue
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

// The login screen shows which backend the build actually targets. Alipay
// cloud URLs are detected automatically; custom domains can set the label:
//   ./gradlew assembleDebug -PorbitApiBaseUrl=https://orbit.example.com/ \
//       -PorbitBackendLabel="uniCloud · 支付宝云"
val orbitBackendLabel: String = providers.gradleProperty("orbitBackendLabel")
    .orElse(
        if (
            orbitApiBaseUrlValue.contains("bspapp.com", ignoreCase = true) ||
            orbitApiBaseUrlValue.contains("cloudbasefunction.cn", ignoreCase = true)
        ) {
            "uniCloud · 支付宝云"
        } else {
            "Spring Boot · MySQL"
        },
    )
    .get()
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.example.orbit"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.orbit"
        minSdk = 24
        targetSdk = 37
        versionCode = 12
        versionName = "2.9.1"

        testInstrumentationRunner = "com.example.orbit.OrbitTestRunner"

        // Public HTTPS endpoint by default (uniCloud Alipay cloud, orbit-api
        // URL化 route). Pass -PorbitApiBaseUrl=... to target another backend.
        buildConfigField("String", "ORBIT_API_BASE_URL", "\"$orbitApiBaseUrl\"")
        buildConfigField("String", "ORBIT_BACKEND_LABEL", "\"$orbitBackendLabel\"")
    }

    buildTypes {
        release {
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
    sourceSets {
        // MigrationTestHelper reads the exported schemas from androidTest assets.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.exifinterface)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    // room-testing ships serializers compiled against serialization-core 1.8,
    // while lifecycle pins 1.7.3 through consistent resolution. Raise the debug
    // runtime so MigrationTestHelper can read exported schemas.
    debugImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
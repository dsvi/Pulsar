plugins {
    id("com.android.application")
    // Compose compiler plugin. Kotlin itself is provided by AGP 9's built-in Kotlin
    // support, so `org.jetbrains.kotlin.android` is intentionally NOT applied here.
    id("org.jetbrains.kotlin.plugin.compose")
}

android {

    compileSdk = 37
    lint {
        disable += "ExpiredTargetSdkVersion"
    }

    defaultConfig {
        applicationId = "ds.pulsar"
        minSdk = 28
        targetSdk = 30 // check EnsurePermissionGranted.kt
        versionCode = 7
        versionName = "7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
    }
    // AGP 9 defaults source/target compatibility to Java 11; Kotlin 2.x dropped the
    // 1.8 target entirely. With built-in Kotlin, jvmTarget defaults to this value.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    namespace = "com.ds.pulsar"
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // navigation
    implementation("androidx.navigation:navigation-compose:2.10.2")

    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    implementation("com.beepiz.blegattcoroutines:blegattcoroutines-core:0.5.0")
    implementation("com.beepiz.blegattcoroutines:blegattcoroutines-genericaccess:0.5.0")
}

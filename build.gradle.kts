// Top-level build file where you can configuration common to all sub-projects/modules.
plugins {
    id("com.android.application") version "9.4.1" apply false
    id("com.android.library") version "9.4.1" apply false
    // Compose compiler plugin; its version must match the Kotlin version used by
    // AGP's built-in Kotlin (see android.kotlinVersion in gradle.properties).
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
}

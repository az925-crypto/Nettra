// Root build — Nettra com.zaaam.nettra
// JDK 21, Gradle 8.12, AGP 8.7.3, Kotlin 2.0.21 — lihat 03_VERSION_MATRIX.md
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

tasks.register("clean") {
    delete(rootProject.layout.buildDirectory)
}

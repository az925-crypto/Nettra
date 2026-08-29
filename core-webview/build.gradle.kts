plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.zaaam.nettra.webview"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    kotlin { jvmToolchain(17) }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.webkit)
    implementation(project(":core-privacy"))
    implementation(project(":core-network-inspector"))
    implementation(project(":core-tabs"))
    testImplementation(libs.junit)
}

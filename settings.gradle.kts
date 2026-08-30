pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    // gradle/libs.versions.toml auto-discovered as 'libs' — jangan panggil from() lagi (Gradle 9: Multiple 'from' invocations)
}

rootProject.name = "nettra"
include(":app")
include(":core-webview")
include(":core-privacy")
include(":core-search")
include(":core-tabs")
include(":feature-browser-ui")

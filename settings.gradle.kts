pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "archivekeep"

include(
    ":common",
    ":cli",
    ":app-core",
    ":app-desktop",
)

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "archivekeep"

include(
    "cli",
    "gui"
)

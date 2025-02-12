pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "archivekeep"

include(
    ":files",
    ":cli",
    ":app-core",
    ":app-desktop",
)

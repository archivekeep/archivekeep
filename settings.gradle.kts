pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()

        maven(uri(rootDir.resolve("./offline-repository")))
    }
}

rootProject.name = "archivekeep"

include(
    ":utils",
    ":files",
    ":cli",
    ":app-core",
    ":app-desktop",
)

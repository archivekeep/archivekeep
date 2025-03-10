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

project(":utils").projectDir = file("modules/utils")
project(":files").projectDir = file("modules/files")
project(":cli").projectDir = file("modules/cli")
project(":app-core").projectDir = file("modules/app-core")
project(":app-desktop").projectDir = file("modules/app-desktop")

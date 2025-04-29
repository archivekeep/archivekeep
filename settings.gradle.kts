pluginManagement {
    repositories {
        val isFDroidBuild = providers.gradleProperty("isFDroidBuild").orNull.toBoolean()

        mavenCentral()
        gradlePluginPortal()
        google()

        if (!isFDroidBuild) {
            maven(uri(rootDir.resolve("./offline-repository")))
        }
    }
}

rootProject.name = "archivekeep"

include(
    ":utils",
    ":files",
    ":cli",
    ":app-android",
    ":app-core",
    ":app-desktop",
    ":app-ui",
)

project(":utils").projectDir = file("modules/utils")
project(":files").projectDir = file("modules/files")
project(":cli").projectDir = file("modules/cli")
project(":app-android").projectDir = file("modules/app-android")
project(":app-core").projectDir = file("modules/app-core")
project(":app-desktop").projectDir = file("modules/app-desktop")
project(":app-ui").projectDir = file("modules/app-ui")

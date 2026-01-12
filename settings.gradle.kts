pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()

        maven(uri(rootDir.resolve("./offline-repository")))
    }
}

rootProject.name = "archivekeep"

include(
    ":utils",
    ":files",
    ":files-driver-filesystem",
    ":files-driver-grpc",
    ":files-driver-s3",
    ":files-test",
    ":cli",
    ":app-android",
    ":app-core",
    ":app-desktop",
    ":app-ui",
)

project(":utils").projectDir = file("modules/utils")
project(":files").projectDir = file("modules/files")
project(":files-driver-filesystem").projectDir = file("modules/files-driver-filesystem")
project(":files-driver-grpc").projectDir = file("modules/files-driver-grpc")
project(":files-driver-s3").projectDir = file("modules/files-driver-s3")
project(":files-test").projectDir = file("modules/files-test")
project(":cli").projectDir = file("modules/cli")
project(":app-android").projectDir = file("modules/app-android")
project(":app-core").projectDir = file("modules/app-core")
project(":app-desktop").projectDir = file("modules/app-desktop")
project(":app-ui").projectDir = file("modules/app-ui")

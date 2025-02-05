pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm").version(extra["kotlin.version"] as String)
        kotlin("plugin.serialization").version(extra["kotlin.version"] as String)
        kotlin("kapt").version(extra["kotlin.version"] as String)

        id("org.jetbrains.compose").version(extra["compose.version"] as String)

        id("com.google.protobuf").version(extra["protobuf.version"] as String)

        id("org.jlleitschuh.gradle.ktlint").version("12.1.2")
    }
}

rootProject.name = "archivekeep"

include(
    ":common",
    ":cli",
    ":app-core",
    ":app-desktop",
)

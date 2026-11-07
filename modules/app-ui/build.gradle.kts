plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    alias(libs.plugins.metro)
}

kotlin {
    jvmToolchain(21)

    android {
        compileSdk = 37
        namespace = "org.archivekeep.app.ui"
        minSdk = 30

        androidResources {
            enable = true
        }
    }

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":app-core"))

                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)

                implementation(libs.compose.components.resources)
                implementation(libs.compose.runtime)

                implementation(libs.compose.material)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.material3)

                implementation(libs.kotlinx.serialization.json)

                implementation(libs.compose.grid)
                implementation(libs.compose.filekit)
                implementation(libs.compose.resources.tabler.icons)

                api(libs.compose.material3.windowsizeclass)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("com.google.accompanist:accompanist-permissions:0.37.2")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)

                implementation(libs.kotlin.test)
                implementation(libs.compose.ui.test.junit4.desktop)

                implementation(libs.kotest.assertions.core)

                implementation(libs.s3.mock)
                implementation(libs.s3.mock.testcontainers)

                implementation(project(":files-driver-filesystem"))
            }
        }
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.res?.addStaticSourceDirectory("src/commonMain/composeResources")
    }
}

tasks.withType<Test> {
    environment["SCREENSHOTS_BUILD_OUTPUT"] = "${project.layout.buildDirectory.get()}/generated-ui-screenshots"
}

compose.resources {
    publicResClass = true
    packageOfResClass = "org.archivekeep.ui.resources"
    generateResClass = always
}


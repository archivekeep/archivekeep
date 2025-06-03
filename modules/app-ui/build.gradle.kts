plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    id("com.android.library")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget()

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":app-core"))

                implementation(compose.ui)
                implementation(compose.foundation)

                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.runtime)

                implementation(compose.material)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)

                implementation(compose.preview)

                implementation(libs.kotlinx.serialization.json)

                implementation(libs.compose.grid)
                implementation(libs.compose.filekit)
                implementation(libs.compose.resources.tabler.icons)

                api(libs.compose.material3.windowsizeclass)
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.7.2")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.10.1")

                implementation("com.google.accompanist:accompanist-permissions:0.37.2")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                implementation(compose.desktop.currentOs)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.preview)

                implementation(libs.kotlin.test)
                implementation(compose.desktop.uiTestJUnit4)
            }
        }
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

android {
    compileSdk = 35
    namespace = "org.archivekeep.app.ui"

    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].res.srcDirs("src/commonMain/composeResources")
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].resources.srcDirs("src/commonMain/composeResources")

    defaultConfig {
        minSdk = 30
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

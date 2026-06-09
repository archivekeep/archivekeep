import com.gitlab.svg2ico.Svg2PngTask
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.compose)

    alias(libs.plugins.metro)

    alias(libs.plugins.ktlint)
}

val gradleOfflineBuildExtra by
    configurations.creating {
        isCanBeConsumed = true
        isCanBeResolved = true
    }

dependencies {
    implementation(project(":app-core"))
    implementation(project(":app-ui"))

    implementation(compose.desktop.currentOs)
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "org.archivekeep.app.desktop.demo.MainKt"

        nativeDistributions {
            packageName = "archivekeep-desktop-demo"
            packageVersion = (project.properties["version"]!! as String)

            linux {
                modules("jdk.security.auth")
                modules("jdk.unsupported")

                installationPath = "/usr"
            }

            copyright = "GPL-3.0-or-later"
            licenseFile.set(rootProject.file("LICENSE"))
        }

        buildTypes.release.proguard.isEnabled = false
    }
}

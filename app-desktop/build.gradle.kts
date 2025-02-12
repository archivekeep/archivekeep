import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.compose)

    alias(libs.plugins.ktlint)
}

group = "org.archivekeep"
version = libs.versions.archivekeep.get()

dependencies {
    implementation(project(":files"))
    implementation(project(":app-core"))

    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)

    implementation(compose.preview)

    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation(compose.preview)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.compose.grid)
    implementation(libs.compose.filekit)
    implementation(libs.compose.resources.tabler.icons)

    implementation(libs.guava)

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

kotlin {
    jvmToolchain(17)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "org.archivekeep.app.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm)

            packageName = "archivekeep-desktop"
            packageVersion = libs.versions.archivekeep.get()

            linux.rpmPackageVersion = createRPMPackageVersion()

            linux {
                modules("jdk.security.auth")
                modules("jdk.unsupported")

                installationPath = "/usr"
            }

            copyright = "AGPLv3"
            licenseFile.set(rootProject.file("LICENSE"))
        }

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
    }
}

fun createRPMPackageVersion() =
    libs.versions.archivekeep
        .get()
        .replace('-', '_')

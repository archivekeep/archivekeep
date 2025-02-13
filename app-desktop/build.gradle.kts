import com.gitlab.svg2ico.Svg2PngTask
import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.compose)

    alias(libs.plugins.svg2ico)

    alias(libs.plugins.ktlint)
}

dependencies {
    implementation(project(":files"))
    implementation(project(":app-core"))

    implementation(compose.components.resources)
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

val pngIconTask =
    tasks
        .register("pngIcon", Svg2PngTask::class) {
            source = project.file("src/main/composeResources/drawable/ic_app.svg")
            width = 192
            height = 192
            destination = project.layout.buildDirectory.file("generated/svg2png/ic_app_192.png")
        }.get()

compose.desktop {
    application {
        mainClass = "org.archivekeep.app.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm)

            packageName = "archivekeep-desktop"
            packageVersion = libs.versions.archivekeep.get()

            linux.rpmPackageVersion = createRPMPackageVersion()

            linux {
                dependsOn(pngIconTask)

                modules("jdk.security.auth")
                modules("jdk.unsupported")

                installationPath = "/usr"

                iconFile.set(pngIconTask.destination)
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

publishing {
    publications.named<MavenPublication>("maven") {
        artifactId = "archivekeep-desktop-application"

        afterEvaluate {
            tasks.named<Jar>("packageReleaseUberJarForCurrentOS").get().let { releaseUberJar ->
                artifact(releaseUberJar) {
                    classifier = "${releaseUberJar.archiveAppendix.get()}-uber"
                }
            }
        }

        pom {
            name = "ArchiveKeep Desktop Application"
            description = "Desktop application for archive management."
        }
    }
}

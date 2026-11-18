import com.gitlab.svg2ico.Svg2PngTask
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.compose)

    alias(libs.plugins.metro)

    alias(libs.plugins.svg2ico)

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

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.compose.components.resources)
    implementation(compose.desktop.currentOs)

    gradleOfflineBuildExtra("com.guardsquare:proguard-gradle:7.8.0")
    gradleOfflineBuildExtra("org.jetbrains.compose:gradle-plugin-internal-jdk-version-probe:${libs.versions.compose.get()}")
}

kotlin {
    jvmToolchain(21)
}

val generatedGoogleDriveClientSecret =
    layout.buildDirectory.file("generated/googleDriveClientSecret/google-drive-client-secret.txt")
val encryptedGoogleDriveClientSecret =
    providers
        .gradleProperty("googleDriveClientSecret")
        .orElse(providers.environmentVariable("GOOGLE_DRIVE_CLIENT_SECRET"))
        .orElse("WiG3UtICrJIDMi2l6EVlqf4hdZY1uVPavORc6maH35eFTCN5y3C0WDIQKw2Eg6civzqXipksHnlLoVboWeYEGA==")
val generateGoogleDriveClientSecret =
    tasks.register("generateGoogleDriveClientSecret") {
        inputs.property("encryptedGoogleDriveClientSecret", encryptedGoogleDriveClientSecret)
        outputs.file(generatedGoogleDriveClientSecret)

        doLast {
            val encryptedBytes = Base64.getDecoder().decode(encryptedGoogleDriveClientSecret.get())
            require(encryptedBytes.size > 16 && (encryptedBytes.size - 16) % 16 == 0) {
                "Google Drive client secret must be a Base64-encoded IV and AES-CBC payload"
            }

            val key =
                "8a5a6db6f987e7130912fecfaca02c2e3b4a4b0b620376200ccd2bffa7c7748f"
                    .chunked(2)
                    .map { it.toInt(16).toByte() }
                    .toByteArray()
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                IvParameterSpec(encryptedBytes.copyOfRange(0, 16)),
            )
            val clientSecret =
                cipher
                    .doFinal(encryptedBytes.copyOfRange(16, encryptedBytes.size))
                    .toString(Charsets.UTF_8)
            require(clientSecret.isNotBlank()) { "Google Drive client secret cannot be blank" }

            generatedGoogleDriveClientSecret.get().asFile.apply {
                parentFile.mkdirs()
                writeText(clientSecret, Charsets.UTF_8)
            }
        }
    }

sourceSets.main {
    resources.srcDir(generatedGoogleDriveClientSecret.get().asFile.parentFile)
}

tasks.named("processResources") {
    dependsOn(generateGoogleDriveClientSecret)
}

tasks.named("sourcesJar") {
    dependsOn(generateGoogleDriveClientSecret)
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
            packageVersion = (project.properties["version"]!! as String)

            linux.rpmPackageVersion = createRPMPackageVersion()

            linux {
                dependsOn(pngIconTask)

                modules("jdk.security.auth")
                modules("jdk.unsupported")

                installationPath = "/usr"

                iconFile.set(pngIconTask.destination)
            }

            copyright = "GPL-3.0-or-later"
            licenseFile.set(rootProject.file("LICENSE"))
        }

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
    }
}

fun createRPMPackageVersion() = (project.properties["version"]!! as String).replace('-', '_')

publishing {
    publications.named<MavenPublication>("maven") {
        artifactId = "archivekeep-desktop-application"

        pom {
            name = "ArchiveKeep Desktop Application"
            description = "Desktop application for archive management."
        }
    }
}

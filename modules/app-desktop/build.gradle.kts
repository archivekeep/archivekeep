import com.gitlab.svg2ico.Svg2PngTask
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.compose)

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

    implementation(compose.components.resources)
    implementation(compose.desktop.currentOs)

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation(compose.desktop.uiTestJUnit4)

    gradleOfflineBuildExtra("com.guardsquare:proguard-gradle:7.7.0")
    gradleOfflineBuildExtra("org.jetbrains.compose:gradle-plugin-internal-jdk-version-probe:1.8.0")
}

kotlin {
    jvmToolchain(17)
}

tasks.named<ProcessResources>("processResources") {
    filesMatching("org/archivekeep/app/desktop/application.properties") {
        expand(
            mapOf(
                "version" to (project.properties["version"]!! as String),
            ),
        )
    }
}

tasks.named<Test>("test") {
    useJUnit()

    environment["SCREENSHOTS_BUILD_OUTPUT"] = "${project.layout.buildDirectory.get()}/generated-ui-screenshots"
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

            copyright = "AGPLv3"
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

// inspired from: https://github.com/JetBrains/compose-multiplatform/issues/1174#issuecomment-2480503879

val proguardOutputFile =
    project.layout.buildDirectory
        .file("generated/proguard/collected-rules.pro")
        .get()

val proguardCollectionTask =
    tasks.register("generateProGuardMergedConfig") {
        val cp: FileCollection = configurations["runtimeClasspath"]

        inputs
            .files(cp)
            .withPropertyName("classpath")
            .withPathSensitivity(PathSensitivity.RELATIVE)

        outputs.file(proguardOutputFile)

        doLast {
            proguardOutputFile.asFile.bufferedWriter().use { proguardFileWriter ->
                sourceSets.main
                    .get()
                    .runtimeClasspath
                    .filter { it.extension == "jar" }
                    .forEach { jar ->
                        val zip = zipTree(jar)
                        zip.matching { include("META-INF/**/proguard/*.pro") }.forEach {
                            proguardFileWriter.appendLine("########   ${jar.name} ${it.name}")
                            proguardFileWriter.appendLine(it.readText())
                        }
                        zip.matching { include("META-INF/services/*") }.forEach {
                            it.readLines().forEach { cls ->
                                val rule = "-keep class $cls"
                                proguardFileWriter.appendLine(rule)
                            }
                        }
                    }
            }
        }
    }

tasks.withType(org.jetbrains.compose.desktop.application.tasks.AbstractProguardTask::class.java) {
    dependsOn(proguardCollectionTask)

    compose.desktop.application.buildTypes.release.proguard {
        configurationFiles.from(proguardOutputFile)
    }
}

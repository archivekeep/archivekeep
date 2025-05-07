import io.github.jwharm.flatpakgradlegenerator.FlatpakGradleGeneratorTask

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.serialization) apply false

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.flatpak.gradle.generator)
    alias(libs.plugins.svg2ico) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.ktlint) apply false

    id("java")
    id("signing")
    id("maven-publish")

    idea
}

subprojects {
    group = "org.archivekeep"

    version = (rootProject.properties["version"]!! as String)

    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()

        // for Flatpak offline no-network-access build
        maven(uri(rootDir.resolve("offline-repository")))
    }
}

subprojects {
    if (name in listOf("app-android", "app-ui", "app-core")) {
        return@subprojects
    }

    apply {
        plugin("java")
        plugin("maven-publish")
        plugin("signing")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        withJavadocJar()
        withSourcesJar()
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                pom {
                    packaging = "jar"
                    url = "https://archivekeep.com/"

                    licenses {
                        license {
                            name = "GNU Affero General Public License, Version 3"
                            url = "https://www.gnu.org/licenses/agpl-3.0.txt"
                        }
                    }
                    developers {
                        developer {
                            id = "kravemir"
                            name = "Miroslav Kravec"
                            email = "kravec.miroslav@gmail.com"
                        }
                    }
                    scm {
                        connection = "scm:git:https://github.com/archivekeep/archivekeep.git"
                        developerConnection = "scm:git:ssh://git@github.com:archivekeep/archivekeep.git"
                        url = "https://github.com/archivekeep/archivekeep"
                    }
                }

                repositories {
                    maven {
                        name = "LocalOutput"
                        url = uri(rootProject.layout.buildDirectory.dir("maven-publish-output"))
                    }
                    maven {
                        name = "CentralSnapshots"
                        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
                        credentials(PasswordCredentials::class)
                    }
                }
            }
        }
    }

    signing {
        useGpgCmd()

        sign(publishing.publications["maven"])
    }
}

allprojects {
    if (name in listOf("app-android")) {
        return@allprojects
    }

    apply {
        plugin("io.github.jwharm.flatpak-gradle-generator")
    }

    tasks.named<FlatpakGradleGeneratorTask>("flatpakGradleGenerator") {
        outputFile = project.layout.buildDirectory.file("flatpak/dependencies-sources.json")
        downloadDirectory = "./offline-repository"

        includeConfigurations =
            setOf(
                "commonCompileClasspath",
                "commonRuntimeClasspath",
                "desktopCompileClasspath",
                "desktopRuntimeClasspath",
                "kotlinBuildToolsApiClasspath",
                "kotlinCompilerClasspath",
                "kotlinCompilerPluginClasspath",
                "kotlinCompilerPluginClasspathMain",
                "annotationProcessor",
                "compileClasspath",
                "runtimeClasspath",
                "protobuf",
                "protobufToolsLocator_grpc",
                "protobufToolsLocator_grpckt",
                "protobufToolsLocator_protoc",
                "gradleOfflineBuildExtra",
            )
        excludeConfigurations = setOf("kotlinNativeBundleConfiguration")
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

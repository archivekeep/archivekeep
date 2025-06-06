plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)

    id("com.android.library")

    alias(libs.plugins.ktlint)
}

kotlin {
    androidTarget()

    jvm("desktop")

    jvmToolchain(17)

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":files"))
                api(project(":files-driver-s3"))

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.guava)

                implementation(libs.jcip.annotations)
                implementation(libs.jose.jwt)

                api(libs.androidx.datastore)
                api(libs.androidx.datastore.preferences)

                implementation(libs.kfswatch)
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
                implementation(libs.oshi.core)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.junit.jupiter)

                implementation(libs.kotest.assertions.core)

                implementation(libs.s3.mock)
                implementation("org.testcontainers:minio:1.21.1")
                implementation(libs.testcontainers.junit.jupiter)
            }
        }
    }
}

tasks.withType<ProcessResources> {
    filesMatching("org/archivekeep/app/core/application.properties") {
        expand(
            mapOf(
                "version" to (project.properties["version"]!! as String),
            ),
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

android {
    compileSdk = 35
    namespace = "org.archivekeep.app.core"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

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

// publishing {
//    publications.named<MavenPublication>("maven") {
//        artifactId = "archivekeep-application-core"
//
//        pom {
//            name = "ArchiveKeep Application Core"
//            description = "Library with Application Core for ArchiveKeep application."
//        }
//    }
// }

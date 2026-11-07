plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)

    id("com.android.kotlin.multiplatform.library")

    alias(libs.plugins.metro)

    alias(libs.plugins.ktlint)
}

kotlin {
    jvmToolchain(21)

    android {
        compileSdk = 37
        namespace = "org.archivekeep.app.core"
    }

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":files"))
                api(project(":files-driver-filesystem"))
                api(project(":files-driver-grpc"))
                api(project(":files-driver-s3"))

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.guava.jre)
                implementation(libs.guava.android)

                implementation(libs.jcip.annotations)
                implementation(libs.jose.jwt)

                api(libs.androidx.datastore)
                api(libs.androidx.datastore.preferences)

                implementation(libs.kfswatch)
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.10.1")
                api("androidx.appcompat:appcompat:1.7.0")
                api("androidx.core:core-ktx:1.15.0")

                implementation("com.google.accompanist:accompanist-permissions:0.37.3")
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
                implementation(libs.s3.mock.testcontainers)
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

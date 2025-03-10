plugins {
    id("java-library")

    alias(libs.plugins.kotlin.jvm)

    alias(libs.plugins.ktlint)
}

group = "org.archivekeep"
version = libs.versions.archivekeep.get()

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

publishing {
    publications.named<MavenPublication>("maven") {
        artifactId = "archivekeep-utils"

        pom {
            name = "ArchiveKeep Utils"
            description = "Library with general purpose utility classes and other reusable code."
        }
    }
}

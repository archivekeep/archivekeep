plugins {
    id("java-library")

    alias(libs.plugins.kotlin.jvm)

    alias(libs.plugins.ktlint)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.guava)
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

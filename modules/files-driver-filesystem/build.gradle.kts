plugins {
    id("java-library")

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.ktlint)
}

dependencies {
    api(project(":files"))
    api(project(":utils"))

    implementation(libs.kfswatch)

    compileOnly(libs.apache.tomcat.annotations)

    testImplementation(project(":files-test"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions.core)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

publishing {
    publications.named<MavenPublication>("maven") {
        artifactId = "archivekeep-files-driver-filesystem"

        pom {
            name = "ArchiveKeep Files Filesystem Driver"
            description = "Library implementing filesystem driver for repositories."
        }
    }
}

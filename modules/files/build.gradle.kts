plugins {
    id("java-library")

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.ktlint)
}

dependencies {
    api(project(":utils"))

    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)

    api(libs.jose.jwt)

    api(libs.guava.jre)
    api(libs.guava.android)

    compileOnly(libs.apache.tomcat.annotations)

    testImplementation(project(":files-driver-filesystem"))
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
        artifactId = "archivekeep-files"

        pom {
            name = "ArchiveKeep Files"
            description = "Library providing files management and core functionality, and base API(s)."
        }
    }
}

plugins {
    id("java-library")

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.ktlint)
}

dependencies {
    api(project(":utils"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.jose.jwt)

    implementation(libs.guava.jre)
    implementation(libs.guava.android)

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
        artifactId = "archivekeep-files"

        pom {
            name = "ArchiveKeep Files"
            description = "Library providing files management and core functionality, and base API(s)."
        }
    }
}

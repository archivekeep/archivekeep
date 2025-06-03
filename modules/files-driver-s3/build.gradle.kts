plugins {
    id("java-library")

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.ktlint)
}

dependencies {
    api(project(":files"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    api(libs.s3.sdk)

    testImplementation(project(":files-test"))

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.jupiter)

    testImplementation(libs.s3.mock)
    testImplementation(libs.s3.mock.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

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

    implementation(libs.oshi.core)

    implementation(libs.guava)

    implementation(libs.jcip.annotations)
    implementation(libs.jose.jwt)

    api(libs.androidx.datastore)
    api(libs.androidx.datastore.preferences)

    implementation(libs.kfswatch)

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

kotlin {
    jvmToolchain(17)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications.named<MavenPublication>("maven") {
        artifactId = "archivekeep-application-core"

        pom {
            name = "ArchiveKeep Application Core"
            description = "Library with Application Core for ArchiveKeep application."
        }
    }
}

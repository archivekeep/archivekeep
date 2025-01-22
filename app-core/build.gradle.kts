plugins {
    id("java-library")

    kotlin("jvm")
    kotlin("plugin.serialization")

    id("org.jlleitschuh.gradle.ktlint")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(project(":common"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("com.github.oshi:oshi-core:6.6.5")

    implementation("com.google.guava:guava:33.3.0-jre")

    implementation("net.jcip:jcip-annotations:1.0")
    implementation("com.nimbusds:nimbus-jose-jwt:9.46")

    api("androidx.datastore:datastore:1.1.1")
    api("androidx.datastore:datastore-preferences:1.1.1")

    implementation("io.github.irgaly.kfswatch:kfswatch:1.3.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

kotlin {
    jvmToolchain(17)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

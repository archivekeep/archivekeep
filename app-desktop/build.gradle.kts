import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")

    id("org.jetbrains.compose")

    id("org.jlleitschuh.gradle.ktlint")
}

group = "org.archivekeep"
version = "0.2.1-SNAPSHOT"

dependencies {
    implementation(project(":common"))
    implementation(project(":app-core"))

    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)

    implementation(compose.preview)

    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation(compose.preview)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("br.com.devsrsouza.compose.icons:tabler-icons:1.1.1")
    implementation("com.cheonjaeung.compose.grid:grid:2.0.0")
    implementation("io.github.vinceglb:filekit-compose:0.8.3")

    implementation("com.google.guava:guava:33.3.0-jre")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

kotlin {
    jvmToolchain(17)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "org.archivekeep.app.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            linux {
                modules("jdk.security.auth")
                modules("jdk.unsupported")
            }

            packageName = "archivekeep-gui"
            packageVersion = "1.0.0"
        }
    }
}

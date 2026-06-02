plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.ksp)

    id("com.android.library")

    alias(libs.plugins.ktlint)
}

kotlin {
    androidTarget()

    jvm("desktop")

    jvmToolchain(17)

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":files"))
                api(project(":utils"))

                implementation(libs.kfswatch)

                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.bundled)

                compileOnly(libs.apache.tomcat.annotations)
            }
        }
        val androidMain by getting {}
        val desktopMain by getting {}
        val desktopTest by getting {
            dependencies {
                implementation(project(":files-test"))
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.junit.jupiter)
                implementation(libs.kotest.assertions.core)
            }
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

android {
    compileSdk = 35
    namespace = "org.archivekeep.files.driver.filesystem"

    defaultConfig {
        minSdk = 30
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

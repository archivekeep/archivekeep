plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.android.application)

    alias(libs.plugins.compose)
}

kotlin {
    androidTarget()
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(":app-core"))
                implementation(project(":app-ui"))

                implementation(libs.kotlinx.serialization.json)

                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.material)
                implementation(libs.compose.components.resources)
            }
        }
    }
}

android {
    compileSdk = 35
    namespace = "org.archivekeep.app.android"

    defaultConfig {
        applicationId = "org.archivekeep.ArchiveKeep"
        minSdk = 30
        targetSdk = 35

        versionCode = (project.properties["versionCode"]!! as String).toInt()
        versionName = (project.properties["version"]!! as String)
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }
    packaging {
        resources {
            pickFirsts.add("META-INF/AL2.0")
            pickFirsts.add("META-INF/LGPL2.1")

            pickFirsts.add("META-INF/io.netty.versions.properties")

            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/INDEX.LIST")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

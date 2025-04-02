plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.multiplatform)

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

                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.components.resources)
            }
        }
    }
}

android {
    compileSdk = 34
    namespace = "org.archivekeep.app.android"

    defaultConfig {
        applicationId = "org.archivekeep.ArchiveKeep"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = libs.versions.archivekeep.get()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    packaging {
        resources {
            pickFirsts.add("META-INF/AL2.0")
            pickFirsts.add("META-INF/LGPL2.1")
        }
    }
    buildFeatures {
        compose = true
    }
}

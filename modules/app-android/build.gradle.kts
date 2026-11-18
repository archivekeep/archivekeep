import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.android.application)

    alias(libs.plugins.compose)

    alias(libs.plugins.metro)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
val googleDriveClientId =
    localProperties
        .getProperty("googleDriveClientId")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "367084333033-n8de4dv4lg43g8rmotk6c0op179egapu.apps.googleusercontent.com"
check(Regex("\\d+-[A-Za-z0-9-]+\\.apps\\.googleusercontent\\.com").matches(googleDriveClientId)) {
    "googleDriveClientId must be a valid Google OAuth client ID ending in .apps.googleusercontent.com"
}
val googleDriveClientIdPrefix = googleDriveClientId.removeSuffix(".apps.googleusercontent.com")
val googleDriveRedirectScheme = "com.googleusercontent.apps.$googleDriveClientIdPrefix"

dependencies {
    implementation(project(":app-core"))
    implementation(project(":app-ui"))

    implementation(libs.appauth)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.compose.components.resources)
}

android {
    compileSdk = 37
    namespace = "org.archivekeep.app.android"

    defaultConfig {
        applicationId = "org.archivekeep.ArchiveKeep"
        minSdk = 30
        targetSdk = 35

        versionCode = (project.properties["versionCode"]!! as String).toInt()
        versionName = (project.properties["version"]!! as String)

        buildConfigField("String", "GOOGLE_DRIVE_CLIENT_ID", "\"$googleDriveClientId\"")
        manifestPlaceholders["appAuthRedirectScheme"] = googleDriveRedirectScheme
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
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
}

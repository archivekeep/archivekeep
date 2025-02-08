plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.serialization) apply false

    alias(libs.plugins.compose) apply false

    alias(libs.plugins.protobuf) apply false

    alias(libs.plugins.ktlint) apply false

    idea
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

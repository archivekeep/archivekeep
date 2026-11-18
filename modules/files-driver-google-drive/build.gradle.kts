plugins {
    id("java-library")

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.ktlint)
}

val gradleOfflineBuildExtra by
    configurations.creating {
        isCanBeConsumed = true
        isCanBeResolved = true
    }

val gradleOfflineBuildExtra2 by
    configurations.creating {
        isCanBeConsumed = true
        isCanBeResolved = true
    }

dependencies {
    api(project(":files"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    api(libs.google.drive.sdk)

    // The flatpak-gradle-generator plugin fails to resolve these BOMs because
    // the Google parent POMs define <properties> after <dependencyManagement>,
    // so property references like ${project.http.version} are unresolved when parsed.
    // google-api-client-parent uses 2.0.0, google-oauth-client-parent uses 1.43.3.
    // Two separate configs are needed for the same artifact with different versions,
    // since Gradle deduplicates within a single configuration.
    gradleOfflineBuildExtra("com.google.http-client:google-http-client-bom:2.0.0")
    gradleOfflineBuildExtra("com.google.oauth-client:google-oauth-client-bom:1.36.0")
    gradleOfflineBuildExtra("com.google.auth:google-auth-library-bom:1.30.0")
    gradleOfflineBuildExtra2("com.google.http-client:google-http-client-bom:1.43.3")
}

kotlin {
    jvmToolchain(21)
}

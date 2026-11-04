import kim.jade.kotlinx.protobuf.gradle.KotlinxProtobufExtension
import kim.jade.kotlinx.protobuf.gradle.proto

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.kotlinx.protobuf)

    alias(libs.plugins.ksp)

    id("com.android.library")

    alias(libs.plugins.ktlint)
}

extensions.configure<KotlinxProtobufExtension> {
    protocVersion = "3.25.5"
}

val gradleOfflineBuildExtra by
    configurations.creating {
        isCanBeConsumed = true
        isCanBeResolved = true
    }

kotlin {
    androidTarget()

    jvm("desktop")

    jvmToolchain(21)

    sourceSets {
        val commonMain by getting {
            proto {
                builtin("java") {
                    option("lite")
                    wireToNothing()
                }
            }

            dependencies {
                api(project(":files"))
                api(project(":utils"))

                implementation(libs.kfswatch)

                implementation(libs.protobuf.kotlin.lite)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.bundled)

                implementation(libs.rocksdb.multiplatform)

                compileOnly(libs.apache.tomcat.annotations)
            }
        }
        val androidMain by getting {
            kotlin.srcDir("${layout.buildDirectory.get()}/generated/sources/kotlinx-protobuf-protoc/commonMain/java")
        }
        val desktopMain by getting {
            kotlin.srcDir("${layout.buildDirectory.get()}/generated/sources/kotlinx-protobuf-protoc/commonMain/java")
        }
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

    // for kspKotlinDesktop
    gradleOfflineBuildExtra("com.google.devtools.ksp:symbol-processing-aa-embeddable:2.3.7")
    gradleOfflineBuildExtra("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }
}

tasks.configureEach {
    if (name.startsWith("compile") || name.startsWith("ksp") || name.startsWith("runKtlint")) {
        dependsOn("generateCommonMainProtoDescriptorSet")
        dependsOn("generateCommonMainProto")
    }
}

tasks.named<JavaCompile>("compileDesktopMainJava") {
    dependsOn("generateCommonMainProto")
    source(
        layout.buildDirectory.dir("generated/sources/kotlinx-protobuf-protoc/commonMain/java"),
    )
    classpath += configurations.getByName("desktopCompileClasspath")
}

android {
    sourceSets {
        getByName("main") {
            java.srcDir(layout.buildDirectory.dir("generated/sources/kotlinx-protobuf-protoc/commonMain/java"))
        }
    }
}

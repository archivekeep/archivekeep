plugins {
    id("java-library")

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.protobuf)

    alias(libs.plugins.ktlint)
}

dependencies {
    api(project(":utils"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.grpc.protobuf.lite)
    implementation(libs.grpc.core)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.okhttp)

    api(libs.grpc.kotlin.stub)

    implementation(libs.protobuf.kotlin.lite)

    implementation(libs.kfswatch)

    compileOnly(libs.apache.tomcat.annotations)

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.25.3" }

    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.asProvider().get()}"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }

    generateProtoTasks {
        all().forEach {
            it.builtins {
                named("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
            it.plugins {
                create("grpc") {
                    option("lite")
                }
                create("grpckt") {
                    option("lite")
                }
            }
        }
    }
}

publishing {
    publications.named<MavenPublication>("maven") {
        artifactId = "archivekeep-files"

        pom {
            name = "ArchiveKeep Files"
            description = "Library providing files management and core functionality, and base API(s)."
        }
    }
}

plugins {
    id("java-library")

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.protobuf)

    alias(libs.plugins.ktlint)
}

dependencies {
    api(project(":files"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.grpc.protobuf.lite)
    implementation(libs.grpc.core)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.okhttp)

    implementation(libs.jose.jwt)

    implementation(libs.guava.jre)
    implementation(libs.guava.android)

    api(libs.grpc.kotlin.stub)

    implementation(libs.protobuf.kotlin.lite)

    implementation(libs.kfswatch)

    compileOnly(libs.apache.tomcat.annotations)

    testImplementation(project(":files-test"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions.core)
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

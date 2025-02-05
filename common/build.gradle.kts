plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")

    id("java-library")
    id("com.google.protobuf")

    id("org.jlleitschuh.gradle.ktlint")
}

group = "org.archivekeep"
version = "1.0-SNAPSHOT"

dependencies {
    runtimeOnly("io.grpc:grpc-netty-shaded:1.66.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("io.grpc:grpc-protobuf:1.66.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    api("io.grpc:grpc-stub:1.66.0")
    api("io.grpc:grpc-kotlin-stub:1.4.1")

    implementation("com.google.protobuf:protobuf-kotlin:3.25.3")

    implementation("io.github.irgaly.kfswatch:kfswatch:1.3.0")

    compileOnly("org.apache.tomcat:annotations-api:6.0.53")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.25.3" }

    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.62.2"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }

    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}

ktlint {
    filter {
        exclude { element ->
            val path = element.file.path

            path.contains("/generated/")
        }
    }
}

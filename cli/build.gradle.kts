import org.asciidoctor.gradle.jvm.AsciidoctorTask

plugins {
    application

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)

    alias(libs.plugins.asciidoctor)

    alias(libs.plugins.ktlint)
}

group = "org.archivekeep"
version = libs.versions.archivekeep.get()

dependencies {
    implementation(project(":files"))

    implementation(libs.picocli)
    kapt(libs.picocli.codegen)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

kapt {
    arguments {
        arg("project", "${project.group}/${project.name}")
    }
}

application {
    mainClass = "org.archivekeep.cli.MainKt"
}

tasks.withType(Jar::class).configureEach {
    manifest {
        attributes["Main-Class"] = "org.archivekeep.cli.MainKt"
    }
}

val generateManpageAsciiDoc =
    tasks.register<JavaExec>("generateManpageAsciiDoc") {
        dependsOn(tasks.named("classes"))

        group = "Documentation"
        description = "Generate AsciiDoc manpage"

        classpath(configurations.compileClasspath, configurations.named("kapt"), sourceSets.main.get().runtimeClasspath)
        mainClass = "picocli.codegen.docgen.manpage.ManPageGenerator"

        args("org.archivekeep.cli.MainCommand", "--outdir=${project.layout.buildDirectory.get()}/generated-picocli-docs", "-v")
    }

tasks {
    "asciidoctor"(AsciidoctorTask::class) {
        dependsOn(generateManpageAsciiDoc)

        sourceDir(file("${project.layout.buildDirectory}/generated-picocli-docs"))
        setOutputDir(file("${project.layout.buildDirectory}/docs"))

        logDocuments = true

        outputOptions {
            backends("manpage", "html5")
        }
    }
}

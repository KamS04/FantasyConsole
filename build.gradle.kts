import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "ca.kam.metainstructions"
version = "1.0"

repositories {
    mavenCentral()
}

application {
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.6.0")
}

val jar: Jar by tasks

abstract class NoDependenciesJar: Jar()
tasks.register<NoDependenciesJar>("noDependenciesJar") {
    archiveBaseName.set("${project.name}-no-deps")
    with(jar)
}

abstract class FatJar: Jar()
tasks.register<FatJar>("fatJar") {
    val dependencies = configurations.runtimeClasspath
        .get()
        .map(::zipTree)

    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    with(jar)
}

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
    idea
}

group = "ca.kam.fantasyconsole"
version = "1.0"

repositories {
    mavenCentral()
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("ca.kam.fantasyconsole.MainKt")
}

fun DependencyHandlerScope.compileRuntimeJarLibs(vararg jarNames: String) {
    val runtimeFiles = jarNames.map { "C:\\home\\code\\Kotlin\\$it\\build\\libs\\$it-1.0.jar" }
    val compileFiles = jarNames.map { "C:\\home\\code\\Kotlin\\$it\\build\\libs\\$it-no-deps-1.0.jar" }

    compileOnly(files(compileFiles))
    runtimeOnly(files(runtimeFiles))
}

dependencies {
    compileRuntimeJarLibs(
        "MetaInstructions",
        "VMHardwareLibraries"
    )
    implementation(
        files(
            "C:\\home\\code\\Kotlin\\ParserCombinators\\build\\libs\\ParserCombinators-1.0.jar",
        )
    )
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
}

val jar: Jar by tasks
jar.apply {
    manifest.attributes.apply {
        put("Main-Class", "ca.kam.fantasyconsole.JarMainKt")
    }

    val dependencies = configurations.runtimeClasspath
        .get()
        .map(::zipTree)

    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val run: JavaExec by tasks
run.apply {
    standardInput = System.`in`
}
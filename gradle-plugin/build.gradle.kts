import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`

    // TODO: Remove when Gradle supports Kotlin 2.3.X (https://docs.gradle.org/current/userguide/compatibility.html#kotlin)
    id("org.jetbrains.kotlin.jvm") version "2.3.10"

    alias(libs.plugins.indra.base)
    alias(libs.plugins.indra.licenser)
    alias(libs.plugins.indra.gradle.plugin)
    alias(libs.plugins.gradle.publish)
}

description = "Gradle plugin for Jakery, a compile time reflection library"

repositories {
    gradlePluginPortal()
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
    }
}

indra {
    javaVersions {
        target(25)
        minimumToolchain(25)
        strictVersions(true)
    }
}

indraPluginPublishing {
    website("https://github.com/WasabiThumb/jakery")
    plugin(
        "jakery-gradle",
        "io.github.wasabithumb.jakery.gradle.JakeryPlugin",
        "Jakery",
        "${project.description}",
        listOf("jakery", "reflection", "runtime")
    )
}

tasks.jar {
    manifest.attributes["Library-Version"] = "${rootProject.version}"
}

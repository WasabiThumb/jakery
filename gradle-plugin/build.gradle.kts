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

val internals: Project = project(":internals")

dependencies {
    compileOnly(internals)

    // Shadow integration
    compileOnly("com.gradleup.shadow:shadow-gradle-plugin:9.3.1")
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

gradlePlugin {
    vcsUrl = "https://github.com/WasabiThumb/jakery.git"
}

val generateLibraryVersionFile = tasks.register("generateLibraryVersionFile") {
    val file = this.temporaryDir.resolve("libraryVersion.txt")
    outputs.file(file)
    doFirst {
        file.writeText("${rootProject.version}\n")
    }
}

tasks.processResources {
    // Generate the META-INF/jakery/libraryVersion.txt file
    val file = generateLibraryVersionFile.map { it.outputs.files.singleFile }
    dependsOn(generateLibraryVersionFile)
    into("META-INF/jakery") {
        from(file)
    }

    // Shade internals
    val internalsCompile = internals.tasks.compileJava
    dependsOn(internalsCompile)
    from(internalsCompile.map { it.outputs.files })
}

tasks.jar {
    manifest.attributes["Library-Version"] = "${rootProject.version}"
}

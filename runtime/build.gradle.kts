
plugins {
    id("java-library")
    alias(libs.plugins.indra.base)
    alias(libs.plugins.indra.publishing)
    alias(libs.plugins.indra.licenser)
}

description = "Runtime components for Jakery"

repositories {
    mavenCentral()
}

dependencies {
    api(libs.jspecify)
    api(libs.annotations)
    implementation(project(":internals"))
}

indra {
    github("WasabiThumb", "jakery")
    apache2License()
    javaVersions {
        target(8)
        minimumToolchain(25)
        strictVersions(true)
    }
    configurePublications {
        artifactId = "jakery-runtime"
        pom.developers {
            developer {
                id = "wasabithumb"
                name = "Xavier Pedraza"
                url = "https://github.com/WasabiThumb"
            }
        }
    }
}

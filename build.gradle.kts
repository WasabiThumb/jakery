
plugins {
    alias(libs.plugins.indra.base) apply false
    alias(libs.plugins.indra.licenser)
    alias(libs.plugins.indra.publishing) apply false
    alias(libs.plugins.indra.sonatype)
}

allprojects {
    group = "io.github.wasabithumb"
    version = "0.2.0"
}

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file("license_header.txt"))
    newLine(true)
}

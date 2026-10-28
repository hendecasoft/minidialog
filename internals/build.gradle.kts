
plugins {
    id("java-library")
    alias(libs.plugins.indra)
    alias(libs.plugins.indra.licenser)
}

description = "Internal components for MiniDialog (DO NOT PUBLISH)"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(libs.jspecify)
    compileOnly(libs.annotations)
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.minimessage)
}

indra {
    apache2License()
    github("hendecasoft", "minidialog")
    javaVersions {
        target(25)
        minimumToolchain(25)
    }
}

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file("license_header.txt"))
    newLine(true)
}

tasks.withType(Jar::class) {
    archiveBaseName = "minidialog-internals"
}

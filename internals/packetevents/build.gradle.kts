
plugins {
    id("java-library")
    alias(libs.plugins.indra)
    alias(libs.plugins.indra.licenser)
}

description = "Extensions to MiniDialog internals using PacketEvents (DO NOT PUBLISH)"

repositories {
    mavenCentral()
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    api(libs.jspecify)
    api(libs.annotations)
    api(project(":api"))
    implementation(project(":internals"))
    compileOnly(libs.adventure.api)
    compileOnly(libs.packetevents.api)
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
    archiveBaseName = "minidialog-internals-packetevents"
}

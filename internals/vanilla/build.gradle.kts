
plugins {
    id("java-library")
    alias(libs.plugins.indra)
    alias(libs.plugins.indra.licenser)
    alias(libs.plugins.ttdev)
}

description = "Extensions to MiniDialog internals using Mojang classes (DO NOT PUBLISH)"

repositories {
    mavenCentral()
    ttdev.repository()
}

dependencies {
    api(libs.jspecify)
    api(libs.annotations)
    api(project(":api"))
    implementation(project(":internals"))
    compileOnly(libs.adventure.api)
    compileOnly(ttdev.minecraftClient(libs.versions.minecraft))
    compileOnly(ttdev.minecraftServer(libs.versions.minecraft))
    compileOnly("net.kyori:adventure-platform-mod-shared:6.9.0")
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
    archiveBaseName = "minidialog-internals-vanilla"
}

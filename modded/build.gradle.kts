import tasks.ResolvePeersTask

plugins {
    id("java-library")
    alias(libs.plugins.indra)
    alias(libs.plugins.indra.licenser)
    alias(libs.plugins.ttdev)
    alias(libs.plugins.indra.publishing)
}

description = "The MiniDialog UI library for Fabric/NeoForge"

repositories {
    mavenCentral()
    ttdev.repository()
}

dependencies {
    api(libs.jspecify)
    api(libs.annotations)
    api(project(":api"))
    compileOnly(project(":internals"))
    compileOnly(project(":internals:vanilla"))
    compileOnly(ttdev.minecraftClient(libs.versions.minecraft))
    compileOnly(ttdev.minecraftServer(libs.versions.minecraft))
    compileOnly(libs.adventure.api)
    compileOnly("net.kyori:adventure-platform-mod-shared:6.9.0")
}

indra {
    apache2License()
    forgejo(uri("https://git.hendeca.moe/"), "x", "minidialog")
    publishReleasesTo("hendecaReleases", "https://maven.hendeca.moe/releases")
    publishSnapshotsTo("hendecaSnapshots", "https://maven.hendeca.moe/snapshots")
    javaVersions {
        target(25)
        minimumToolchain(25)
    }
    configurePublications {
        artifactId = "minidialog-modded"
        pom {
            name = "MiniDialog Fabric/NeoForge"
            inceptionYear = "2026"
            url = "https://github.com/hendecasoft/minidialog/"
            organization {
                name = "hendecasoft"
                url = "https://hendeca.moe/"
            }
            developers {
                developer {
                    id = "wasabithumb"
                    name = "Xavier Pedraza"
                    organization = "hendecasoft"
                    roles = setOf("author")
                    timezone = "America/New_York"
                    email = "xpedraza542@gmail.com"
                }
            }
        }
    }
}

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file("license_header.txt"))
    newLine(true)
}

ttdev {
    javadoc = true
    sources = true
}

tasks.withType(Jar::class) {
    archiveBaseName = "minidialog-modded"
}

val resolvePeers = tasks.register("resolvePeers", ResolvePeersTask::class) {
    description = "Flatten peer projects for use in processResources"
    from(project(":internals"), project(":internals:vanilla"))
}

tasks.processResources {
    dependsOn(resolvePeers)
    from(resolvePeers.map { it.outputs.files.singleFile })
}

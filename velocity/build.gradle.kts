
plugins {
    id("java-library")
    alias(libs.plugins.indra)
    alias(libs.plugins.indra.licenser)
}

description = "The MiniDialog UI library for Velocity"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

// Projects to bundle
val internals = project(":internals")
val packetevents = project(":internals:packetevents")

dependencies {
    api(libs.jspecify)
    api(libs.annotations)
    api(project(":api"))
    compileOnly(internals)
    compileOnly(packetevents)
    compileOnly(libs.velocity.api)
    compileOnly(libs.packetevents.api)
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
        artifactId = "minidialog-velocity"
        pom {
            name = "MiniDialog Velocity"
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

tasks.withType(Jar::class) {
    archiveBaseName = "minidialog-velocity"
}

tasks.processResources {
    listOf(internals, packetevents).forEach { p ->
        dependsOn(p.tasks.assemble)
        from(p.sourceSets.main.map { it.output.classesDirs.singleFile })
    }
}

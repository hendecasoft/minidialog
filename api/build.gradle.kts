
plugins {
    id("java-library")
    alias(libs.plugins.indra)
    alias(libs.plugins.indra.licenser)
    alias(libs.plugins.indra.publishing)
}

description = "API components for MiniDialog"

repositories {
    mavenCentral()
}

dependencies {
    api(libs.jspecify)
    api(libs.annotations)
    compileOnly(libs.adventure.api)
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
        artifactId = "minidialog-api"
        pom {
            name = "MiniDialog API"
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
    archiveBaseName = "minidialog-api"
}

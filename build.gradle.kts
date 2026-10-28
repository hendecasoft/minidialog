
allprojects {
    group = "moe.hendeca"
    version = "0.1.0-SNAPSHOT"

    tasks.withType(JavaCompile::class) {
        options.isFork = true
    }
}

plugins {
    alias(libs.plugins.indra) apply false
    alias(libs.plugins.indra.licenser)
    alias(libs.plugins.indra.publishing) apply false
    alias(libs.plugins.indra.git) apply false
}

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file("license_header.txt"))
    newLine(true)
}

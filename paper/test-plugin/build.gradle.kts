import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
    id("java")
    alias(libs.plugins.indra)
    alias(libs.plugins.shadow)
    alias(libs.plugins.resource.factory.paper)
    alias(libs.plugins.run.paper)
}

description = "Paper test plugin for MiniDialog"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(libs.jspecify)
    implementation(libs.annotations)
    implementation(project(":paper"))
    implementation(project(":internals"))
    compileOnly(libs.paper.api)
}

indra {
    apache2License()
    github("hendecasoft", "minidialog")
    javaVersions {
        target(25)
        minimumToolchain(25)
    }
}

paperPluginYaml {
    main = "moe.hendeca.minidialog.plugin.MiniDialogTestPlugin"
    name = "MiniDialogTestPlugin"
    prefix = "MDTP"
    authors = listOf("hendecasoft")
    website = "https://github.com/hendecasoft/minidialog/"
    apiVersion = "26.1"
    load = BukkitPluginYaml.PluginLoadOrder.STARTUP
}

tasks.processResources {
    from(rootProject.layout.projectDirectory.dir("samples")) {
        into("dialogs")
    }
}

tasks.withType(Jar::class) {
    archiveBaseName = "minidialog-paper-test-plugin"
}

tasks.shadowJar {
    archiveClassifier = ""
}

tasks.runServer {
    dependsOn(tasks.shadowJar)
    minecraftVersion("26.1.2")
    pluginJars(tasks.shadowJar.map { it.outputs.files.singleFile })
}

package tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer

/**
 * Consumes a set of peer
 * projects and flattens their
 * Java classes and resources
 * into a single directory
 */
@CacheableTask
abstract class ResolvePeersTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    protected abstract val dirs: Property<FileCollection>

    @get:Input
    abstract val ignorePatterns: SetProperty<Regex>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        this.dirs.convention(project.files())
        this.ignorePatterns.convention(setOf())
        this.outputDirectory.convention(project.layout.buildDirectory.dir("tmp/${this.name}"))
    }

    //

    fun from(vararg projects: Project) {
        val fc = this.project.objects.fileCollection()
        for (project in projects) {
            val java = project.extensions.findByType(JavaPluginExtension::class.java) ?: continue
            this.dependsOn(project.tasks.named("assemble"))
            fc.from(java.sourceSets.named("main")
                .map { s -> this.project.files(s.output.classesDirs, s.output.resourcesDir) }
                .orElse(this.project.provider { this.project.files() }))
        }
        this.dirs.set(fc)
    }

    @TaskAction
    fun resolve() {
        val target = this.outputDirectory.asFile.get().toPath()
        if (Files.exists(target)) target.clearDir() else Files.createDirectories(target)

        val prefix = StringBuilder()
        for (dir in this.dirs.get().files) {
            val src = dir.toPath()
            if (!Files.exists(src)) continue
            prefix.setLength(0)
            this.resolveIn(prefix, src, target)
        }
    }

    private fun resolveIn(prefix: StringBuilder, src: Path, target: Path) {
        val prefixLength = prefix.length
        Files.list(src).use { stream ->
            stream.forEach { child ->
                val name = child.fileName.toString()
                val targetChild = target.resolve(name)
                prefix.setLength(prefixLength)
                prefix.append(name)
                val path = prefix.toString()
                if (this.isIgnored(path)) return@forEach
                prefix.append('/')

                if (Files.isDirectory(child)) {
                    if (!Files.exists(targetChild)) Files.createDirectories(targetChild)
                    prefix.append('/')
                    this.resolveIn(prefix, child, targetChild)
                } else if (Files.isRegularFile(child)) {
                    this.blit(path, child, targetChild)
                } else {
                    this.logger.warn("Skipping \"$path\" (not a regular file or directory)")
                }
            }
        }
    }

    private fun isIgnored(path: String): Boolean {
        return this.ignorePatterns
            .map { it.any { r -> r.matches(path) } }
            .get()
    }

    private fun blit(path: String, from: Path, to: Path) {
        val toParent = to.parent
        if (!Files.exists(toParent)) Files.createDirectories(toParent)
        if (Files.exists(to)) {
            this.logger.warn("Skipping \"$path\" (already exists)")
            return
        }
        Files.copy(from, to)
    }

    //

    companion object {

        private fun Path.clearDir() {
            if (!Files.isDirectory(this)) return
            this.useList { child ->
                child.clearDir()
                Files.delete(child)
            }
        }

        private fun Path.useList(action: Consumer<Path>) {
            Files.list(this).use { stream ->
                stream.forEach { action.accept(it) }
            }
        }

    }

}
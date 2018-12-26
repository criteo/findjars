package com.criteo.gradle.findjars

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

open class FindJarsTask : DefaultTask() {

    @Input
    @Optional
    @Option(option = "contains",
            description = "Regular expression matched against the path of a file inside a jar (default= '.*class').")
    var jarFilter = ".*class"

    @Input
    @Optional
    @Option(option = "configuration",
            description = "Configuration to look for jars (default=compile).")
    var configuration = "compile"

    @Input
    @Optional
    @Option(option = "find-conflicts",
            description = "Find jars that contain files with the same path (com/criteo/my.class) with different content (default=false).")
    var findConflicts = false

    @TaskAction
    fun run() {
        val jarFileAndEntry = JarFileAndEntry.scan(project, configuration, logger, jarFilter)
        if (findConflicts) {
            reportConflicts(jarFileAndEntry)
        }
        else {
            reportFoundJars(jarFileAndEntry)
        }
    }

    private fun reportFoundJars(jarFileAndEntry: Collection<JarFileAndEntry>) {
        if (jarFileAndEntry.isEmpty()) {
            logger.lifecycle("Did not find any jar")
        } else {
            jarFileAndEntry.map { it.jar }.distinct().forEach {
                logger.lifecycle("- ${it}")
            }
        }
    }

    private fun reportConflicts(jarFileAndEntry: Collection<JarFileAndEntry>) {
        val entries = EntriesInMultipleJars(jarFileAndEntry)
        if (entries.isEmpty()) {
            logger.lifecycle("No conflicts found.")
        } else {
            val factorizedConflicts: Map<ConflictingJars, List<String>> = entries.factorize()
            factorizedConflicts.forEach { key, value ->
                logger.lifecycle("Jars:")
                key.jars.forEach {
                    logger.lifecycle("- $it")
                }
                logger.lifecycle("conflict on: ")
                reportConflictingClasses(value)
            }
        }
    }

    private fun reportConflictingClasses(conflictingClasses: Collection<String>) {
        val length = conflictingClasses.size
        val maxClasses = 5
        val addThreeDots = length > maxClasses
        conflictingClasses.take(maxClasses).forEach {
            logger.lifecycle(" - $it")
        }
        if (addThreeDots) {
            logger.lifecycle(" - ... (${length - maxClasses} more)")
        }
    }

}

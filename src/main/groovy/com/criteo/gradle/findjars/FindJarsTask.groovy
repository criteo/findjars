package com.criteo.gradle.findjars

import com.criteo.gradle.findjars.lookup.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

class FindJarsTask extends DefaultTask {

    @Input
    @Optional
    @Option(option = "contains",
            description = "Regular expression matched against the path of a file inside a jar (default= '.*class').")
    String jarFilter

    @Input
    @Optional
    @Option(option = "configuration",
            description = "Configuration to look for jars (default=compile).")
    String configuration

    FindJarsTask() {
        super()
        setupDefaultOptions()
    }

    @Input
    @Optional
    @Option(option = "find-conflicts",
            description = "Find jars that contain files with the same path (com/criteo/my.class) with different content (default=false).")
    boolean findConflicts

    @TaskAction
    void run() {
        Collection<JarFileAndEntry> jarFileAndEntry = JarsHavingEntriesMatchingFilter.collect(project, logger, jarFilter)
        if (findConflicts) {
            reportConflicts(jarFileAndEntry)
        }
        if (findConflicts != null) {
            reportFoundJars(jarFileAndEntry)
        }
    }

    private void reportFoundJars(Collection<JarFileAndEntry> jarFileAndEntry) {
        if (jarFileAndEntry.isEmpty()) {
            logger.lifecycle("Did not find any jar")
        } else {
            jarFileAndEntry.collect { it.getJarPath() }.unique().each {
                logger.lifecycle("- ${it}")
            }
        }
    }

    private void reportConflicts(Collection<JarFileAndEntry> jarFileAndEntry) {
        Map<String, Set<String>> conflicts = Conflicts.entriesInMultipleJars(jarFileAndEntry)
        if (conflicts.isEmpty()) {
            logger.lifecycle("No conflicts found.")
        } else {
            Map<ConflictingJars, Set<String>> factorizedConflicts = Conflicts.factorize(conflicts)
            for (Map.Entry<ConflictingJars, Set<String>> entry: factorizedConflicts) {
                Set<String> jars = entry.getKey().getJars()
                logger.lifecycle("Jars:")
                jars.each {
                    logger.lifecycle("- $it")
                }
                logger.lifecycle("conflict on: ")
                reportConflictingClasses(entry.getValue())
            }
        }
    }

    private void reportConflictingClasses(Collection<String> conflictingClasses) {
        int length = conflictingClasses.size()
        int maxClasses = 5
        boolean addThreeDots = length > maxClasses
        conflictingClasses.take(maxClasses).each {
            val ->
                logger.lifecycle(" - $val")
        }
        if (addThreeDots) {
            logger.lifecycle(" - ... (${length - maxClasses} more)")
        }
    }

    private void setupDefaultOptions() {
        setupJarFilter()
        setupConfiguration()
    }

    private void setupJarFilter() {
        if (jarFilter == null) {
            jarFilter = ".*\\.class"
        }
    }

    private void setupConfiguration() {
        if (configuration == null) {
            configuration = "compile"
        }
    }

}

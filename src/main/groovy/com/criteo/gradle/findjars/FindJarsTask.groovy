package com.criteo.gradle.findjars

import com.criteo.gradle.findjars.lookup.ConflictingJars
import com.criteo.gradle.findjars.lookup.JarFileAndEntry
import com.criteo.gradle.findjars.lookup.JarFileAndPath
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.internal.impldep.org.apache.commons.codec.digest.DigestUtils

import java.util.jar.JarEntry

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
        Collection<JarFileAndEntry> jarFileAndEntry = com.criteo.gradle.findjars.lookup.JarsHavingEntriesMatchingFilter.collect(project, logger, jarFilter)
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
        Map<String, Set<String>> conflicts = findElementsInMultipleJars(jarFileAndEntry)
        if (conflicts.isEmpty()) {
            logger.lifecycle("No conflicts found.")
        } else {
            Map<ConflictingJars, Set<String>> factorizedConflicts = factorizeConflicts(conflicts)
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

    /**
     * @param entries
     * @return entries having the same path (inside a jar), but different content in different jars.
     */
    private Map<String, Set<String>> findElementsInMultipleJars(Collection<JarFileAndEntry> entries) {
        Map<String, Set<JarFileAndEntry>> entriesToJars = findEntriesWithSamePathInDifferentJars(entries)
        Map<String, Map<Long, JarFileAndPath>> entriesPerDigest = new HashMap<>()
        for (Map.Entry<String, Set<JarFileAndEntry>> entryToJars: entriesToJars) {
            Map<String, Set<JarFileAndPath>> jarsGroupedByEntriesDigest = new HashMap<>()
            for (JarFileAndEntry entry : entryToJars.getValue()) {
                String digest = getDigest(entry.getJarFile(), entry.getJarEntry())
                Set<JarFileAndPath> related = jarsGroupedByEntriesDigest.getOrDefault(digest, new HashSet<>())
                related.add(entry.getJarFile())
                jarsGroupedByEntriesDigest[digest] = related
            }
            entriesPerDigest[entryToJars.getKey()] = jarsGroupedByEntriesDigest
        }
        entriesPerDigest.findAll { className, perDigest ->
            perDigest.keySet().size() > 1
        }.collectEntries { key, value ->
            Collection<Set<JarFileAndPath>> values = value.values()
            Set<String> paths = values.collectMany { val -> val.collect { it.getJarPath() } }
            [(key): paths]
        }
    }

    private Map<String, Set<JarFileAndEntry>> findEntriesWithSamePathInDifferentJars(Collection<JarFileAndEntry> entries) {
        Map<String, Set<JarFileAndEntry>> elementsToJars = new HashMap<>()
        for (JarFileAndEntry entry : entries) {
            String key = entry.getJarEntry().getName()
            Set<JarFileAndEntry> related = elementsToJars.getOrDefault(key, new HashSet<>())
            related.add(entry)
            elementsToJars[key] = related
        }
        elementsToJars.findAll {
            key, value -> value.size() > 1
        }
    }

    private static String getDigest(JarFileAndPath jar, JarEntry entry) {
        return new BufferedInputStream(jar.getJarFile().getInputStream(entry)).withCloseable { input ->
            DigestUtils.sha1Hex(input)
        }
    }

    private Map<ConflictingJars, Collection<String>> factorizeConflicts(Map<String, Set<String>> conflicts) {
        Map<ConflictingJars, Collection<String>> res = new HashMap<>()
        conflicts.each {
            key, value ->
                ConflictingJars newKey = new ConflictingJars(value)
                Collection<String> related = res.getOrDefault(newKey, [])
                related.add(key)
                res[newKey] = related
        }
        res
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

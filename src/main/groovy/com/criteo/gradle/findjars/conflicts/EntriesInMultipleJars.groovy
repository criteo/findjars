package com.criteo.gradle.findjars.conflicts

import com.criteo.gradle.findjars.lookup.JarFileAndEntry
import com.criteo.gradle.findjars.lookup.JarFileAndPath
import org.gradle.internal.impldep.org.apache.commons.codec.digest.DigestUtils

import java.util.jar.JarEntry

class EntriesInMultipleJars {
    private Map<String, Set<String>> entriesInMultipleJars = new HashMap<>()

    EntriesInMultipleJars(Collection<JarFileAndEntry> entries) {
        entriesInMultipleJars = build(entries)
    }

    boolean isEmpty() {
        entriesInMultipleJars.isEmpty()
    }

    Map<ConflictingJars, Set<String>> factorize() {
        Map<ConflictingJars, Collection<String>> res = new HashMap<>()
        entriesInMultipleJars.each {
             key , value ->
            ConflictingJars newKey = new ConflictingJars(value)
            Collection<String> related = res.getOrDefault(newKey, [])
            related.add(key)
            res[newKey] = related
        }
        res
    }

    private static Map<String, Set<String>> build(Collection<JarFileAndEntry> entries) {
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

    private static Map<String, Set<JarFileAndEntry>> findEntriesWithSamePathInDifferentJars(Collection<JarFileAndEntry> entries) {
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

}


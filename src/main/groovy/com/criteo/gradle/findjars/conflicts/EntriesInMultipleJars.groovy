package com.criteo.gradle.findjars.conflicts

import com.criteo.gradle.findjars.lookup.JarFileAndEntry
import com.criteo.gradle.findjars.lookup.JarFileAndPath
import groovy.transform.CompileStatic
import org.gradle.internal.impldep.org.apache.commons.codec.digest.DigestUtils

import java.util.jar.JarEntry

@CompileStatic
class EntriesInMultipleJars {
    private Map<String, Set<String>> entriesInMultipleJars = new HashMap<>()

    EntriesInMultipleJars(Collection<JarFileAndEntry> entries) {
        entriesInMultipleJars = build(entries)
    }

    boolean isEmpty() {
        entriesInMultipleJars.isEmpty()
    }

    Map<ConflictingJars, Set<String>> factorize() {
        Map<ConflictingJars, Set<String>> res = new HashMap<>()
        entriesInMultipleJars.each {
             key , value ->
            ConflictingJars newKey = new ConflictingJars(value)
            Set<String> related = res.getOrDefault(newKey, new HashSet<>())
            related.add(key)
            res[newKey] = related
        }
        (Map<ConflictingJars, Set<String>>)res.collectEntries {key, value -> [key: value.toSet()] }
    }

    private static Map<String, Set<String>> build(Collection<JarFileAndEntry> entries) {
        Map<String, Set<JarFileAndEntry>> entriesToJars = findEntriesWithSamePathInDifferentJars(entries)
        Map<String, Map<String, Set<JarFileAndPath>>> entriesPerDigest = new HashMap<>()
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
        (Map<String, Set<String>>) entriesPerDigest.findAll { className, perDigest ->
            perDigest.keySet().size() > 1
        }.collectEntries { key, value ->
            Collection<Set<JarFileAndPath>> values = value.values()
            Set<String> paths = values.collectMany { val -> val.collect { it.getJarPath() } }.toSet()
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


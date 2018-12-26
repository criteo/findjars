package com.criteo.gradle.findjars

data class ConflictingJars private constructor (val jars: List<String>) {
    companion object {
        fun of(conflictingJars: Collection<String>): ConflictingJars {
            return ConflictingJars(conflictingJars.sorted())
        }
    }
}

class EntriesInMultipleJars {
    private val entriesInMultipleJars: Map<String, Set<String>>

    constructor(entries: Collection<JarFileAndEntry>) {
        entriesInMultipleJars = of(entries)
    }

    fun isEmpty(): Boolean {
        return entriesInMultipleJars.isEmpty()
    }

    fun factorize(): Map<ConflictingJars, List<String>> {
        return entriesInMultipleJars
                .toList()
                .groupBy { ConflictingJars.of(it.second) }
                .mapValues { it.value.map { pathAndJar -> pathAndJar.first }.toSet().sorted() }
    }

    companion object {
        private fun of(entries: Collection<JarFileAndEntry>): Map<String, Set<String>> {
            return filterPathsHavingDifferentDigest(groupPathsByDigest(groupEntriesByPath(entries)))
        }

        private fun groupPathsByDigest(entriesByPath: Map<String, Set<JarFileAndEntry>>): Map<String, Map<String, Set<JarFileAndPath>>> {
            return entriesByPath.mapValues { groupEntriesByDigest(it.value) }
        }

        private fun groupEntriesByPath(entries: Collection<JarFileAndEntry>): Map<String, Set<JarFileAndEntry>> {
            return entries
                    .groupBy { it.entry.getName() }
                    .mapValues { it.value.toSet() }
                    .filter {
                        it.value.size > 1
                    }
        }

        private fun filterPathsHavingDifferentDigest(entriesByDigest: Map<String, Map<String, Set<JarFileAndPath>>>): Map<String, Set<String>> {
            return entriesByDigest
                    .filter {
                        it.value.keys.size > 1
                    }.mapValues { entry ->
                        entry.value.values.flatMap { it.map { x -> x.jarPath } }.toSet()
                    }
        }

        private fun groupEntriesByDigest(entries: Collection<JarFileAndEntry>): Map<String, Set<JarFileAndPath>>
        {
            return entries
                    .groupBy { it.getDigest() }
                    .mapValues { it.value.map {entry -> entry.jar }.toSet() }
        }

    }

}


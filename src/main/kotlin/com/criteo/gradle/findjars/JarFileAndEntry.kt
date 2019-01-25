package com.criteo.gradle.findjars

import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.BufferedInputStream
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile

data class JarFileAndPath(val jarFile: JarFile, val jarPath: String) {
    constructor(jarPath: String) : this(JarFile(jarPath), jarPath)
}

data class JarFileAndEntry(val jar: JarFileAndPath, val entry: JarEntry) {

    fun getDigest(): String {
        return BufferedInputStream(jar.jarFile.getInputStream(entry)).use { input ->
            DigestUtils.sha1Hex(input)
        }
    }

    companion object {
        fun scan(project: Project, configuration: String, logger: Logger, jarFilter: String): Collection<JarFileAndEntry> {
            val jarFilterRegex = Regex(jarFilter)
            val res = mutableListOf<JarFileAndEntry>()
            if (project.configurations.filter { it.name == configuration }.isEmpty()) {
                return res
            }
            for (path in project.configurations.getByName(configuration).files.map { it.path }) {
                if (!File(path).exists() || !path.endsWith(".jar")) {
                    continue
                }
                try {
                    val jar = JarFileAndPath(path)
                    val entries = jar.jarFile.entries();
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.getName()
                        if (filterJarName(name, jarFilterRegex)) {
                            res.add(JarFileAndEntry(jar, entry))
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Could not open ${path}", e)
                }
            }
            return res.toList()
        }

        private fun filterJarName(name: String, jarFilter: Regex): Boolean {
            return name.matches(jarFilter)
        }

    }
}
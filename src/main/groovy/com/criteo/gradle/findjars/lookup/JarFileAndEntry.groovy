package com.criteo.gradle.findjars.lookup

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger

import java.util.jar.JarEntry

@CompileStatic
class JarFileAndEntry {
    private JarFileAndPath jar
    private JarEntry entry

    static Collection<JarFileAndEntry> scan(Project project,
                                            String configuration,
                                            Logger logger,
                                            String jarFilter) {
        Collection<JarFileAndEntry> res = new ArrayList<>()
        if (((Collection<Configuration>)project.configurations).findAll { it.name == configuration }.isEmpty()) {
            return res
        }
        for (String path : project.configurations.getByName(configuration).files*.path) {
            if (!new File(path).exists() || !path.endsWith(".jar")) {
                continue
            }
            try {
                JarFileAndPath jar = new JarFileAndPath(path);
                Enumeration<JarEntry> entries = jar.getJarFile().entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (filterJarName(name, jarFilter)) {
                        res.add(new JarFileAndEntry(jar, entry))
                        continue
                    }
                }
            } catch (Exception e) {
                logger.error("Could not open ${path}", e)
            }
        }
        res
    }

    private static boolean filterJarName(String name, String jarFilter) {
        return name.matches(jarFilter)
    }

    private JarFileAndEntry(JarFileAndPath jar, JarEntry entry) {
        this.jar = jar
        this.entry = entry
    }

    JarFileAndPath getJarFile() {
        this.jar
    }

    JarEntry getJarEntry() {
        this.entry
    }

    String getJarPath() {
        this.jar.getJarPath()
    }
}

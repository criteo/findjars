package com.criteo.gradle.findjars.lookup

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger

import java.util.jar.JarEntry

class JarsHavingEntriesMatchingFilter {

    static Collection<JarFileAndEntry> collect(Project project,
                                               Configuration configuration, String jarFilter,
                                               Logger logger)
    {
        Collection<JarFileAndEntry> res = new ArrayList<>()
        if (project.configurations.findAll { it.name == configuration }.isEmpty()) {
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

}

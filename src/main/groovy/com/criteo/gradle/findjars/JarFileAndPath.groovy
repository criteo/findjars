package com.criteo.gradle.findjars

import java.util.jar.JarFile

class JarFileAndPath {
    private JarFile jarFile
    private String jarPath

    JarFileAndPath(String jarPath) {
        this.jarPath = jarPath
        this.jarFile = new JarFile(jarPath)
    }

    String getJarPath() {
        jarPath
    }

    JarFile getJarFile() {
        jarFile
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        JarFileAndPath that = (JarFileAndPath) o

        if (jarFile != that.jarFile) return false
        if (jarPath != that.jarPath) return false

        return true
    }

    @Override
    int hashCode() {
        int result
        result = (jarFile != null ? jarFile.hashCode() : 0)
        result = 31 * result + (jarPath != null ? jarPath.hashCode() : 0)
        return result
    }
}


package com.criteo.gradle.findjars.lookup


import java.util.jar.JarEntry

class JarFileAndEntry {
    private JarFileAndPath jar
    private JarEntry entry

    JarFileAndEntry(JarFileAndPath jar, JarEntry entry) {
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

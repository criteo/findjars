package com.criteo.gradle.findjars

import org.gradle.api.Plugin
import org.gradle.api.Project

open class FindJarsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project.tasks) {
            create("findJars", FindJarsTask::class.java) {
                it.description = "Find jars in a given configuration."
                it.group = "debug"
            }
        }
    }
}
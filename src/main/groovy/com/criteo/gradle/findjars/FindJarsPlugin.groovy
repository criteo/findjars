package com.criteo.gradle.findjars

import org.gradle.api.Plugin
import org.gradle.api.Project


class FindJarsPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.with {
            task('findJars', type: FindJarsTask) {
                description = "Find jars in a given configuration."
                group = "debug"
            }
        }
    }
}
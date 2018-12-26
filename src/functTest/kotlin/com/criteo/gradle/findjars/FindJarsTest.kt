package com.criteo.gradle.findjars.lookup

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder


class FindJarsTest {

    @Test
    fun `findJars list jars containing a given entry`() {
        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("findJars", "--contains", "com/google/protobuf/.*class", "--configuration", "compile")
                .withPluginClasspath()
                .build()
        assertThat(result.output).contains("org.apache.hive/hive-exec/3.1.1")
        assertThat(result.output).contains("com.google.protobuf/protobuf-java/3.6.1")
        assertThat(result.output).doesNotContain("org.apache.hive/hive-service-rpc/3.1.1")
    }

    @Test
    fun `findJars finds conflicts`() {
        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("findJars", "--find-conflicts", "--configuration", "compile")
                .withPluginClasspath()
                .build()
        assertThat(result.output).contains(protobufConflicts)
    }

    @Test
    fun `findJars does not find conflicts in jars that do not contain chosen entries`() {
        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("findJars", "--find-conflicts", "--contains", "org/apache/hadoop/hive/.*", "--configuration", "compile")
                .withPluginClasspath()
                .build()
        assertThat(result.output).doesNotContain(protobufConflicts)
    }

    @Test
    fun `findJars does not find any jars using a non existing configurations`() {
        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("findJars", "--configuration", "nonExisting")
                .withPluginClasspath()
                .build()
        assertThat(result.output).startsWith("\n" + """
        > Task :findJars
        Did not find any jar
        """.trimIndent())
    }

    @get:Rule
    val testProjectDir = TemporaryFolder()

    val buildGradleContent = """
        plugins {
           id 'com.criteo.gradle.findjars'
        }

        apply plugin: 'java-library'

        repositories {
            mavenCentral()
        }

        dependencies {
            compile group: 'com.google.protobuf', name: 'protobuf-java', version: '3.6.1'
            compile group: 'org.apache.hive', name: 'hive-exec', version: '3.1.1'
        }
        """

    @Before
    fun setup() {
        testProjectDir.newFile("build.gradle").writeText(buildGradleContent)
    }

    // https://youtrack.jetbrains.com/issue/KT-2425
    // for now, '$' should be written as ${"$"} in string templates
    val protobufConflicts = """
        conflict on: 
         - com/google/protobuf/AbstractMessage${"$"}Builder.class
         - com/google/protobuf/AbstractMessage.class
         - com/google/protobuf/AbstractMessageLite${"$"}Builder${"$"}LimitedInputStream.class
         - com/google/protobuf/AbstractMessageLite${"$"}Builder.class
         - com/google/protobuf/AbstractMessageLite.class
         - ... (233 more)""".trimIndent()
}

package com.criteo.gradle.findjars.lookup

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class FindJarsTest extends Specification {
    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
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
    }

    def "findJars list jars containing a given entry"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('findJars', '--contains', 'com/google/protobuf/.*class', '--configuration', 'compile')
                .withPluginClasspath()
                .build()
        then:
        def output = getOutput(result)
        output.contains("org.apache.hive/hive-exec/3.1.1") && output.contains("com.google.protobuf/protobuf-java/3.6.1") &&
                !output.contains("org.apache.hive/hive-service-rpc/3.1.1")
    }

    def "findJars finds conflicts"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('findJars', '--find-conflicts', '--configuration', 'compile')
                .withPluginClasspath()
                .build()
        then:
        getOutput(result).contains('''
            conflict on: 
             - com/google/protobuf/AbstractMessage$Builder.class
             - com/google/protobuf/AbstractMessage.class
             - com/google/protobuf/AbstractMessageLite$Builder$LimitedInputStream.class
             - com/google/protobuf/AbstractMessageLite$Builder.class
             - com/google/protobuf/AbstractMessageLite.class
             - ... (233 more)'''.stripIndent())
    }

    def "findJars does not find conflicts in jars that do not contain chosen entries"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('findJars', '--find-conflicts', '--contains', 'org/apache/hadoop/hive/.*', '--configuration', 'compile')
                .withPluginClasspath()
                .build()
        then:
        !getOutput(result).contains('''
            conflict on: 
             - com/google/protobuf/DescriptorProtos.class
             - com/google/protobuf/TextFormat$TextGenerator.class
             - com/google/protobuf/GeneratedMessage$ExtendableMessage.class
             - com/google/protobuf/DescriptorProtos$UninterpretedOption$NamePart\$1.class
             - com/google/protobuf/Descriptors$Descriptor.class
             - ... (233 more)'''.stripIndent())
    }

    def "findJars does not find any jars using a non existing configurations"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('findJars', '--configuration', 'nonExisting')
                .withPluginClasspath()
                .build()
        then:
        getOutput(result).startsWith('''
        > Task :findJars
        Did not find any jar
        '''.stripIndent())
    }

    private static String getOutput(def result) {
        result.output.replace("\r", "")
    }

}

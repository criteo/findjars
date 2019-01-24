# Findjars: a tool to debug your gradle classpath

[![Build Status](https://travis-ci.org/criteo/findjars.svg?branch=master)](https://travis-ci.org/criteo/findjars)
Findjars is a tool that helped Criteo debugging classpath issues when migrating from maven to gradle, by finding:
* which jars contain a file identified by its path,
* conflicts among the jars selected above, a conflict being a file with the same path present in two different jars with a different digest.

More precisely, let's take as example the following `build.gradle` that uses the plugin.
```sh
$ cat build.gradle
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
```
We first install gradlew
```
$ gradle wrapper
```
We can find the jars that contain protobuf classes with
```sh
$ ./gradlew findJars --contains "com/google/protobuf/.*class" --configuration compile
...
> Task :findJars
- /path/to/cache/org.apache.hive/hive-exec/3.1.1/1b5b59ec96b6947d34363e7a03b274fb9761cdf7/hive-exec-3.1.1.jar
- /path/to/cache/com.google.protobuf/protobuf-java/3.6.1/d06d46ecfd92ec6d0f3b423b4cd81cb38d8b924/protobuf-java-3.6.1.jar
```

We can check whether there are conflicts there with
```
$ ./gradlew findJars --find-conflicts --configuration compile --contains "com/google/protobuf/.*.class"
...
> Task :findJars
Jars:
- /path/to/cache/com.google.protobuf/protobuf-java/3.6.1/d06d46ecfd92ec6d0f3b423b4cd81cb38d8b924/protobuf-java-3.6.1.jar
- /path/to/cache/org.apache.hive/hive-exec/3.1.1/1b5b59ec96b6947d34363e7a03b274fb9761cdf7/hive-exec-3.1.1.jar
conflict on:
 - com/google/protobuf/AbstractMessage$Builder.class
 - com/google/protobuf/AbstractMessage.class
 - com/google/protobuf/AbstractMessageLite$Builder$LimitedInputStream.class
 - com/google/protobuf/AbstractMessageLite$Builder.class
 - com/google/protobuf/AbstractMessageLite.class
 - ... (233 more)
```

All the conflicts can be found with:
```sh
$ ./gradlew findJars --find-conflicts --configuration compile
...
> Task :findJars
Jars:
- /path/to/cache/org.apache.hive/hive-common/3.1.1/df09ae27a067b0119da823169d35d6515bf0de6c/hive-common-3.1.1.jar
- /path/to/cache/org.apache.hive/hive-exec/3.1.1/1b5b59ec96b6947d34363e7a03b274fb9761cdf7/hive-exec-3.1.1.jar
conflict on:
 - org/apache/hadoop/hive/ant/GenHiveTemplate.class
 - org/apache/hadoop/hive/common/BlobStorageUtils.class
 - org/apache/hadoop/hive/common/CompressionUtils.class
 - org/apache/hadoop/hive/common/CopyOnFirstWriteProperties.class
 - org/apache/hadoop/hive/common/FileUtils$1.class
 - ... (186 more)
Jars:
- /path/to/cache/org.apache.hive/hive-exec/3.1.1/1b5b59ec96b6947d34363e7a03b274fb9761cdf7/hive-exec-3.1.1.jar
- /path/to/cache/org.apache.hive/hive-storage-api/2.7.0/b67545f02e4f821fa154f019ace439d98de715d5/hive-storage-api-2.7.0.jar
conflict on:
 - org/apache/hadoop/hive/common/DiskRangeInfo.class
 - org/apache/hadoop/hive/common/Pool$PoolObjectHelper.class
 - org/apache/hadoop/hive/common/Pool.class
 - org/apache/hadoop/hive/common/ValidCompactorWriteIdList.class
 - org/apache/hadoop/hive/common/ValidReadTxnList.class
 - ... (99 more)
Jars:
- /path/to/cache/org.apache.hive/hive-exec/3.1.1/1b5b59ec96b6947d34363e7a03b274fb9761cdf7/hive-exec-3.1.1.jar
- /path/to/cache/org.apache.orc/orc-core/1.5.1/fa9dda9c085ceb749ef8e766c27d6f14f982f0b9/orc-core-1.5.1.jar
conflict on:
 - org/apache/orc/BinaryColumnStatistics.class
 - org/apache/orc/BooleanColumnStatistics.class
 - org/apache/orc/ColumnStatistics.class
 - org/apache/orc/CompressionCodec$Modifier.class
 - org/apache/orc/CompressionCodec.class
 - ... (380 more)
...
```

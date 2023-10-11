Testcontainers for doris
---

![CI][Badge-CI] [![Nexus (Snapshots)][Badge-Snapshots]][Link-Snapshots] [![Sonatype Nexus (Releases)][Badge-Releases]][Link-Releases]


[Badge-CI]: https://github.com/hjfruit/testcontainers-doris/actions/workflows/scala.yml/badge.svg
[Badge-Snapshots]: https://img.shields.io/nexus/s/hjfruit/testcontainers-doris_3?server=https%3A%2F%2Foss.sonatype.org
[Link-Snapshots]: https://oss.sonatype.org/content/repositories/snapshots/io/github/hjfruit/testcontainers-doris/

[Link-Release]: https://index.scala-lang.org/hjfruit/testcontainers-doris/testcontainers-doris
[Badge-Release]: https://index.scala-lang.org/hjfruit/testcontainers-doris/testcontainers-doris/latest-by-scala-version.svg?platform=jvm

[Badge-Releases]: https://img.shields.io/nexus/r/io.github.hjfruit/testcontainers-doris_3?server=https%3A%2F%2Foss.sonatype.org
[Link-Releases]: https://oss.sonatype.org/content/repositories/releases/io/github/hjfruit/testcontainers-doris_3/

[Testcontainers](https://github.com/testcontainers/testcontainers-java)  is a Java library that supports JUnit tests, providing lightweight, throwaway instances of common databases, Selenium web browsers, or anything else that can run in a Docker container.

Apache [Doris](https://github.com/apache/doris/)  is an easy-to-use, high-performance and real-time analytical database based on MPP architecture, known for its extreme speed and ease of use. It only requires a sub-second response time to return query results under massive data and can support not only high-concurrent point query scenarios but also high-throughput complex analysis scenarios.


## Dependency

Support Java 8+, Scala 3, Scala 2.13 and Scala 2.12

**sbt**:
```scala
"io.github.hjfruit" %% "testcontainers-doris" % 'latest version'
```

**maven**:
```xml
<dependency>
    <groupId>io.github.hjfruit</groupId>
    <artifactId>testcontainers-doris_2.13</artifactId>
    <version>'latest version'</version>
    <scope>test</scope>
</dependency>
```

**gradle**:
```groovy
testImplementation group: 'io.github.hjfruit', name: 'testcontainers-doris_2.13', version: 'latest version'
```

## Usage Instructions

Java example: [SimpleDorisCluster](./examples/src/main/java/testcontainers/containers/SimpleDorisCluster.java)

```

Details:

1. `Doris1FE1BEClusterContainer.scala` creates two container instances: fe, be.
2. `DorisClusterContainer.scala` provides a generic definition, and any number of clusters can be created by implementing its abstraction methods, ports and volumes can be modified.

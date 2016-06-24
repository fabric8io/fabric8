Fabric8 Taiga API
=================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.fabric8/taiga-api/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/io.fabric8/taiga-api/)
[![Javadocs](http://www.javadoc.io/badge/io.fabric8/taiga-api.svg?color=blue)](http://www.javadoc.io/doc/io.fabric8/taiga-api)

This library provides a JAXRS 2.0 based Java client API to [taiga](http://taiga.io/) based issue trackers

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

             <dependency>
                 <groupId>io.fabric8</groupId>
                 <artifactId>taiga-api</artifactId>
                 <version>2.2.101</version>
             </dependency>

### Building

If you clone the source code:

    git clone https://github.com/fabric8io/fabric8.git
    cd fabric8

Then you should be able to build it via:

    cd components/taiga-api
    mvn clean test-compile

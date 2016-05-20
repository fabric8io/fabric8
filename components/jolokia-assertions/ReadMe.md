## Jolokia Assertions

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.fabric8/jolokia-assertions/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/io.fabric8/jolokia-assertions/)
[![Javadocs](http://www.javadoc.io/badge/io.fabric8/jolokia-assertions.svg?color=blue)](http://www.javadoc.io/doc/io.fabric8/jolokia-assertions)

This library provides a bunch of helpful [assertj](http://joel-costigliola.github.io/assertj/) assertions for working with the [jolokia](Jolokia) to perform remote JMX based assertions using an efficient REST/JSON protocol.

[Here is an example of the assertThat(client)](https://github.com/fabric8io/fabric8/blob/master/components/jolokia-assertions/src/test/java/io/fabric8/jolokia/assertions/ExampleTest.java#L47) helper code that is available if you add the **jolokia-assertions** dependency.

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>jolokia-assertions</artifactId>
                <version>2.2.101</version>
                <scope>test</scope>
            </dependency>

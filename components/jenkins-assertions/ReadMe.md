## Jenkins Assertions

This library provides a bunch of helpful [assertj](http://joel-costigliola.github.io/assertj/) assertions for working with [Jenkins](https://jenkins.io/) to assert that jobs get created, builds pass etc.

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>jenkins-assertions</artifactId>
                <version>2.2.101</version>
                <scope>test</scope>
            </dependency>

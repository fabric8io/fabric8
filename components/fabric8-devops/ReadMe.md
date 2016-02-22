## Fabric8 DevOps 

A Java library for working with the Fabric8 DevOps configuration for a project

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>fabric8-devops</artifactId>
                <version>2.2.96</version>
            </dependency>

### API Overview

The main DTO object is [ProjectConfig](https://github.com/fabric8io/fabric8/blob/master/components/fabric8-devops/src/main/java/io/fabric8/devops/ProjectConfig.java#L31).

You can load and save this DTO via the [ProjectConfigs](https://github.com/fabric8io/fabric8/blob/master/components/fabric8-devops/src/main/java/io/fabric8/devops/ProjectConfigs.java#L40) class.

For example:

    ProjectConfig config = ProjectConfigs.loadFromFolder(someFolder);


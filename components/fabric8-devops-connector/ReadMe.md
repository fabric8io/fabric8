## Fabric8 DevOps Connector

A Java library for connecting the various DevOps services like git hosting, chat, issue tracking and jenkins for a project

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>fabric8-devops-connector</artifactId>
                <version>2.2.96</version>
            </dependency>

### API Overview

The main connector API is the [DevOpsConnector](https://github.com/fabric8io/fabric8/blob/master/components/fabric8-devops-connector/src/main/java/io/fabric8/devops/connector/DevOpsConnector.java#L52).

Once configured you can then call the `execute()` method:

    DevOpsConnector connector = new DevOpsConnector();
    connector.setBaseDir(myfolder);
    ...
    connector.execute();


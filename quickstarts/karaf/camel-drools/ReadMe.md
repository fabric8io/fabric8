Drools & Decision Excel Table -Camel Component Example
======================================================

To build this project use

    mvn install

This project includes a unit test, ${package}.CamelContextXmlTest, that shows calling this component

To run this project use

    mvn camel:run -Psimple

to start a Camel project with Drools component &

    mvn camel:run -Pdecision-table

to make a test with a Excel Decision Table

To deploy this project into [Fabric8](http://fabric8.io/gitbook/getStarted.html)

Edit the file etc/org.ops4j.pax.url.mvn.cfg add JBoss maven snapshot repo

    https://repository.jboss.org/nexus/content/repositories/snapshots

Start Fabric8

    <Fabric8 Home>/bin/fabric8 or fabric8.bat

In the Fabric8 console, use the following

    features:addurl mvn:${groupId}/${artifactId}/${version}/xml/features
    features:install camel-drools-example

To see the results tail the Fabric8 log

    tail -f <Fabric8 Home>/data/log/fuse.log



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

To deploy this project into [Fuse ESB](http://fusesource.com/downloads)

Start Fuse ESB

    <Fuse ESB Home>/bin/fuseesb

In the Fuse ESB console, use the following

    FuseESB:karaf@root> features:addurl mvn:${groupId}/${artifactId}/${version}/xml/features
    FuseESB:karaf@root> features:install camel-drools-example

To see the results tail the Fuse ESB log

    tail -f <Fuse ESB Home>/data/log/fuseesb.log



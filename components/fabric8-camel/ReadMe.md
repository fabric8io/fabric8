## Fabric8 Camel

This library provides a framework for working with Pods containing [Apache Camel](http://camel.apache.org/) when running on top of Kubernetes and exposing  [Jolokia](http://jolokia.org/) access to the underlying JMX MBeans for Camel.

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>fabric8-camel</artifactId>
                <version>2.2.96</version>
            </dependency>


### Try an example

If you clone the source code:

    git clone https://github.com/fabric8io/fabric8.git
    cd fabric8

And if you are running a camel context in a replication controller in some namespace then run the following:

    cd components/fabric8-camel
    mvn test-compile exec:java -Dexample.rcName=mycamel -Dexample.rcNamespace=mycamel-staging -Dexample.camelContext=myCamel

Where those parameters are the Replication Controller name running your camel route, the namespace its running inside and the camel context ID to work with.


## Kubernetes Jolokia

This library provides integration between the [kubernetes-api](../kubernetes-api/) and [Jolokia](http://jolokia.org/) so that its easy to work with Java containers which expose Jolokia ports such as the [Fabric8](https://registry.hub.docker.com/repos/fabric8/) containers and [ConSol](https://registry.hub.docker.com/repos/consol/) docker containers.

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>kubernetes-jolokia</artifactId>
                <version>2.2.96</version>
            </dependency>


### Try an example

If you clone the source code:

    git clone https://github.com/fabric8io/fabric8.git
    cd fabric8

And if you are running Kubernetes (e.g. [try run fabric8](http://fabric8.io/guide/getStarted.html)) so that $KUBERNETES_MASTER is pointing to the Kubernetes REST API then the following should work:

    cd components/kubernetes-jolokia
    mvn clean test-compile exec:java

The above should [run this example](https://github.com/fabric8io/fabric8/blob/master/components/kubernetes-jolokia/src/test/java/io/fabric8/kubernetes/jolokia/Example.java#L54) which uses the Kubernetes Client to find all pods matching a particular selector and then show the system CPU load by querying a JMX attribute).

The example then uses the *jolokiaClient()* method to [create a Jolokia client](https://github.com/fabric8io/fabric8/blob/master/components/kubernetes-jolokia/src/test/java/io/fabric8/kubernetes/jolokia/Example.java#L61).

Once you have a Jolokia client you can then use the [Jolokia Client API](http://jolokia.org/reference/html/clients.html#client-java) to read attributes, write attributes and invoke operations.

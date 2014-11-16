## Kubernetes Client API

This library represents a Java [Kubernetes](http://kubernetes.io/) client API using JAXRS 2.0 similar to the [Fabric8 Docker API](https://github.com/fabric8io/fabric8/blob/master/components/docker-api/ReadMe.md).

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>kubernetes-api</artifactId>
                <version>2.0.3</version>
            </dependency>

### Try an example

If you clone the source code:

    git clone https://github.com/fabric8io/fabric8.git
    cd fabric8

And if you are running Kubernetes (e.g. [try run fabric8](http://fabric8.io/v2/getStarted.html)) so that $KUBERNETES_MASTER is pointing to the Kubernetes REST API then the following should work:

    cd components/kubernetes-api
    mvn clean test-compile exec:java

The Example program should start and list some pods.

### API Overview

You use the **KubernetesFactory** to create an instance of [Kubernetes](https://github.com/fabric8io/fabric8/blob/master/components/kubernetes-api/src/main/java/io/fabric8/kubernetes/api/Kubernetes.java#L46) which supports the [Kubernetes REST API](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/DESIGN.md#kubernetes-api-server)

For example:

    KubernetesFactory kubeFactory = new KubernetesFactory();
    Kubernetes kube = kubeFactory.createKubernetes();
    List pods = kube.getPods();

The **KubernetesFactory** defaults to using the **KUBERNETES_MASTER** environment variable.

If your Java code is running inside of a Kubernetes environment the KubernetesFactory will use the environment variables: **KUBERNETES_SERVICE_HOST** and **KUBERNETES_SERVICE_PORT** to communicate with the [kubernetes service](http://fabric8.io/v2/services.html) for the REST API.

If you wish to use a specific URL in your Java code just pass it into the factory constructor (though usually you don't need to!).

    KubernetesFactory kubeFactory = new KubernetesFactory("http://localhost:8585/");
    Kubernetes kube = kubeFactory.createKubernetes();
    List pods = kube.getPods();

To see more of the [Kubernetes API](https://github.com/fabric8io/fabric8/blob/master/components/kubernetes-api/src/main/java/io/fabric8/kubernetes/api/Kubernetes.java#L46) in action [check out this example](https://github.com/fabric8io/fabric8/blob/master/components/kubernetes-api/src/test/java/io/fabric8/kubernetes/api/Example.java#L48)

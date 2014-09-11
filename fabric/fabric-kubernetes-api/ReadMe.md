## Kubernetes Client API

This library represents a Java [Kubernetes](http://kubernetes.io/) client API using JAXRS 2.0 similar to the [Fabric Docker API](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric-docker-api/ReadMe.md).

To try this out, run kubernetes so that $KUBERNETES_MASTER is pointing to the host (or it defaults to http://localhost:8080/) then run

    mvn clean test-compile exec:java

The Example program should start and list some pods.

### API Overview

You use the **KubernetesFactory** to create an instance of [Kubernetes](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric-kubernetes-api/src/main/java/io/fabric8/kubernetes/api/Kubernetes.java#L46) which supports the [Kubernetes REST API](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/DESIGN.md#kubernetes-api-server)

For example:

    KubernetesFactory kubeFactory = new KubernetesFactory();
    Kubernetes kube = kubeFactory.createKubernetes();
    List pods = kube.getPods();

To see more of the [Kubernetes API](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric-kubernetes-api/src/main/java/io/fabric8/kubernetes/api/Kubernetes.java#L46) in action [check out this example](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric-kubernetes-api/src/test/java/io/fabric8/kubernetes/api/Example.java#L54)

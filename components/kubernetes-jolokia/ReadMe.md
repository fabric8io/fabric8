## Kubernetes Jolokia

This simpler library provides integration between the [kubernetes-api](../kubernetes-api/) and [Jolokia](http://jolokia.org/) so that its easy to work with Java containers which expose Jolokia ports such as the [ConSol](https://registry.hub.docker.com/repos/consol/) and [Fabric8](https://registry.hub.docker.com/repos/fabric8/) docker containers.

The best way to start is by [this example](https://github.com/fabric8io/fabric8/blob/master/components/kubernetes-jolokia/src/test/java/io/fabric8/kubernetes/jolokia/Example.java#L54) which uses the Kubernetes Client to find all pods matching a particular selector.

The example then uses the *jolokiaClient()* method to [create a Jolokia client](https://github.com/fabric8io/fabric8/blob/master/components/kubernetes-jolokia/src/test/java/io/fabric8/kubernetes/jolokia/Example.java#L61).

Once you have a Jolokia client you can then use the [Jolokia Client API](http://jolokia.org/reference/html/clients.html#client-java) to read attributes, write attributes and invoke operations.
## Kubernetes Client API

This library represents a Java [Kubernetes](http://kubernetes.io/) client API using JAXRS 2.0.

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>kubernetes-api</artifactId>
                <version>2.2.96</version>
            </dependency>

### Try an example

If you clone the source code:

    git clone https://github.com/fabric8io/fabric8.git
    cd fabric8

And if you are running Kubernetes (e.g. [try run fabric8](http://fabric8.io/guide/getStarted.html)) so that $KUBERNETES_MASTER is pointing to the Kubernetes REST API then the following should work:

    cd components/kubernetes-api
    mvn clean test-compile exec:java

The Example program should start and list some pods.

### API Overview

You use the **DefaultKubernetesClient** to create an instance of [Kubernetes Client](https://github.com/fabric8io/kubernetes-client) which supports the [Kubernetes REST API](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/DESIGN.md#kubernetes-api-server)

For example:

    KubernetesClient kube = new DefaultKubernetesClient();
    PodList pods = kube.pods().list();
    for (Pod pod : pods.getItems()) {
        String name = pod.getMetadata().getName();
        String ip = pod.getStatus().getPodIP();
    }

The **KubernetesClient** defaults to using the **KUBERNETES_MASTER** environment variable.

If your Java code is running inside of a Kubernetes environment the KubernetesClient will default to using **kubernetes.default.svc** (override by setting **KUBERNETES_MASTER**) as the address to communicate with the [kubernetes service](http://fabric8.io/guide/services.html) for the REST API.

If you wish to use a specific URL in your Java code just pass it into the factory constructor (though usually you don't need to!).

    KubernetesClient kube = new DefaultKubernetesClient("http://localhost:8585/");
    PodList pods = kube.pods().list();

To see more of the Kubernetes API in action using the [Kubernetes Client](https://github.com/fabric8io/kubernetes-client) see [this example](https://github.com/fabric8io/fabric8/blob/master/components/kubernetes-api/src/test/java/io/fabric8/kubernetes/api/Example.java#L48)

### Configuration

All configuration is done via the following environment variables:

* `KUBERNETES_MASTER` - the location of the kubernetes master
* `KUBERNETES_NAMESPACE` - the default namespace used on operations
* `KUBERNETES_CERTS_CA_DATA` - the full Kubernetes CA certificate as a string (only this or `KUBERNETES_CERTS_CA_FILE` should be specified)
* `KUBERNETES_CERTS_CA_FILE` - the path to the Kubernetes CA certificate file (only this or `KUBERNETES_CERTS_CA_DATA` should be specified)
* `KUBERNETES_CERTS_CLIENT_DATA` - the full Kubernetes client certificate as a string (only this or `KUBERNETES_CERTS_CLIENT_FILE` should be specified)
* `KUBERNETES_CERTS_CLIENT_FILE` - the path to the Kubernetes client certificate file (only this or `KUBERNETES_CERTS_CLIENT_DATA` should be specified)
* `KUBERNETES_CERTS_CLIENT_KEY_DATA` - the full Kubernetes client private key as a string (only this or `KUBERNETES_CERTS_CLIENT_KEY_FILE` should be specified)
* `KUBERNETES_CERTS_CLIENT_KEY_FILE` - the path to the Kubernetes client private key file (only this or `KUBERNETES_CERTS_CLIENT_KEY_DATA` should be specified)
* `KUBERNETES_TRUST_CERTIFICATES` - whether to trust the Kubernetes server certificate (this is insecure so please try to configure certificates properly via the other environment variables if at all possible)

The `*_DATA` variants take precedence over the `*_FILE` variants.

#### Defaults from OpenShift

If no configuration is supplied through explicit code or environment variables, the `kubernetes-api` library will try to find the current login token and namespace by parsing the users `~/.kube/config` file.

This means that if you use the [OpenShift](http://www.openshift.org/) command line tool `oc` you can login and change projects (namespaces in kubernetes speak) and those will be used by default by the `kubernetes-api` library.

e.g.

```
oc login
oc project cheese
mvn fabric8:apply
```

In the above, if there is no `KUBERNETES_NAMESPACE` environment variable or maven property called `fabric8.apply.namespace` then the `fabric8:apply` goal will apply the Kubernetes resources to the `cheese` namespace.

## Kubernetes Client API

This library represents a Java [Kubernetes](http://kubernetes.io/) client API using JAXRS 2.0 similar to the [Fabric8 Docker API](https://github.com/fabric8io/fabric8/blob/master/components/docker-api/ReadMe.md).

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>kubernetes-api</artifactId>
                <version>2.0.44</version>
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

You use the **KubernetesFactory** to create an instance of [Kubernetes](https://github.com/fabric8io/fabric8/blob/master/components/kubernetes-api/src/main/java/io/fabric8/kubernetes/api/Kubernetes.java#L46) which supports the [Kubernetes REST API](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/DESIGN.md#kubernetes-api-server)

For example:

    KubernetesFactory kubeFactory = new KubernetesFactory();
    Kubernetes kube = kubeFactory.createKubernetes();
    PodListSchema pods = kube.getPods();

The **KubernetesFactory** defaults to using the **KUBERNETES_MASTER** environment variable.

If your Java code is running inside of a Kubernetes environment the KubernetesFactory will use the environment variables: **KUBERNETES_SERVICE_HOST** and **KUBERNETES_SERVICE_PORT** to communicate with the [kubernetes service](http://fabric8.io/guide/services.html) for the REST API.

If you wish to use a specific URL in your Java code just pass it into the factory constructor (though usually you don't need to!).

    KubernetesFactory kubeFactory = new KubernetesFactory("http://localhost:8585/");
    Kubernetes kube = kubeFactory.createKubernetes();
    PodListSchema pods = kube.getPods();

To see more of the [Kubernetes API](https://github.com/fabric8io/fabric8/blob/master/components/kubernetes-api/src/main/java/io/fabric8/kubernetes/api/Kubernetes.java#L46) in action [check out this example](https://github.com/fabric8io/fabric8/blob/master/components/kubernetes-api/src/test/java/io/fabric8/kubernetes/api/Example.java#L48)

### Configuration

All configuration is done via the following environment variables:

* `KUBERNETES_SERVICE_HOST`:`KUBERNETES_SERVICE_PORT` / `KUBERNETES_MASTER` - the location of the kubernetes master
* `KUBERNETES_NAMESPACE` - the default namespace used on operations
* `KUBERNETES_CA_CERTIFICATE_DATA` - the full Kubernetes CA certificate as a string (only this or `KUBERNETES_CA_CERTIFICATE_FILE` should be specified)
* `KUBERNETES_CA_CERTIFICATE_FILE` - the path to the Kubernetes CA certificate file (only this or `KUBERNETES_CA_CERTIFICATE_DATA` should be specified)
* `KUBERNETES_CLIENT_CERTIFICATE_DATA` - the full Kubernetes client certificate as a string (only this or `KUBERNETES_CLIENT_CERTIFICATE_FILE` should be specified)
* `KUBERNETES_CLIENT_CERTIFICATE_FILE` - the path to the Kubernetes client certificate file (only this or `KUBERNETES_CLIENT_CERTIFICATE_DATA` should be specified)
* `KUBERNETES_CLIENT_KEY_DATA` - the full Kubernetes client private key as a string (only this or `KUBERNETES_CLIENT_KEY_FILE` should be specified)
* `KUBERNETES_CLIENT_KEY_FILE` - the path to the Kubernetes client private key file (only this or `KUBERNETES_CLIENT_KEY_DATA` should be specified)
* `KUBERNETES_TRUST_CERT` - whether to trust the Kubernetes server certificate (this is insecure so please try to configure certificates properly via the other environment variables if at all possible)

The `*_DATA` variants take precedence over the `*_FILE` variants.

#### Defaults from OpenShift

If no configuration is supplied through explicit code or environment variables, the `kubernetes-api` library will try to find the current login token and namespace by parsing the users `~/.config/openshift/config` file.

This means that if you use the [OpenShift](http://www.openshift.org/) command line tool `osc` you can login and change projects (namespaces in kubernetes speak) and those will be used by default by the `kubernetes-api` library.

e.g.

```
osc login
osc project cheese
mvn fabric8:apply
```

In the above, if there is no `KUBERNETES_NAMESPACE` environment variable or maven property called `fabric8.apply.namespace` then the `fabric8:apply` goal will apply the Kubernetes resources to the `cheese` namespace.
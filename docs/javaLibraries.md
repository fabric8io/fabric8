## Java Libraries

If you want to write any Java/JVM based tools to interact with [Kubernetes](http://kubernetes.io), [Docker](http://www.docker.com/) or [Etcd](https://github.com/coreos/etcd/blob/master/README.md) we have a number of libraries to help:

### Kubernetes

Kubernetes provides the main REST API for working with the [Kubernetes Platform](http://kubernetes.io). It should provide all you need for writing most services and plugins for Kubernetes.

* [kubernetes-api](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-api) provides a Java API for working with the Kubernetes REST API (pods, replication controllers, services etc)
* [kubernetes-jolokia](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-jolokia) makes it easy to work with the [Jolokia Client API](http://jolokia.org/reference/html/clients.html#client-java) and Java containers running in [Pods](pods.html) inside Kubernetes which expose the Jolokia port
* [kubernetes-template](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-template) provides a simple templating mechanism for generating the Kubernetes JSON files from MVEL templates with parameters from a DTO.

### Docker

* [docker-api](https://github.com/fabric8io/fabric8/tree/master/components/docker-api) for working directly with a docker host over its REST API

### Etcd

* [etcd-api](https://github.com/fabric8io/fabric8/blob/master/components/fabric-etcd/) provides a Java API for working with [etcd](https://github.com/coreos/etcd/blob/master/README.md)
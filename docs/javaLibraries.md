## Java Libraries

If you want to write any Java/JVM based tools to interact with [Kubernetes](http://kubernetes.io), [Docker](http://www.docker.com/) or [Etcd](https://github.com/coreos/etcd/blob/master/README.md) we have a number of libraries to help:

### Kubernetes

Kubernetes provides the main REST API for working with the [Kubernetes Platform](http://kubernetes.io). It should provide all you need for writing most services and plugins for Kubernetes.

* [kubernetes-api](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-api) provides a Java API for working with the Kubernetes REST API (pods, replication controllers, services etc)
* [kubernetes-jolokia](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-jolokia) makes it easy to work with the [Jolokia Client API](http://jolokia.org/reference/html/clients.html#client-java) and Java containers running in [Pods](pods.html) inside Kubernetes which expose the Jolokia port
* [kubernetes-template](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-template) provides a simple templating mechanism for generating the Kubernetes JSON files from MVEL templates with parameters from a DTO

#### Testing

* [arquillian-fabric8](https://github.com/fabric8io/fabric8/tree/master/components/arquillian-fabric8) provides a plugin for [Arquillian](arquillian-fabric8) for integration testing [Apps](apps.html) on top of Kubernetes; using Kubernetes to provision and orchestrate the containers and then making [assertions](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-assertions) that the required resources startup correctly.
* [kubernetes-assertions](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-assertions) provides a set of [assertj](http://joel-costigliola.github.io/assertj/) assertions of the form **assertThat(kubernetesResource)** for working with the [kubernetes-api](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-api)
* [jolokia-assertions](https://github.com/fabric8io/fabric8/tree/master/components/jolokia-assertions) makes it easy to perform assertions on remote JVMs via JMX using  [Jolokia](http://jolokia.org/) over HTTP/JSON


#### CDI

* [fabric8-cdi](cdi.html) provides an easy way to work with Kubernetes [services](service.html) using the CDI Dependency Injection approach
* [fabric8-apt](https://github.com/fabric8io/fabric8/tree/master/fabric8-apt) provides an APT code generator to create a JSON Schema file for each environment variable injected by the [@ConfigProperty](http://deltaspike.apache.org/documentation/configuration.html) annotation from [deltaspike](http://deltaspike.apache.org/) - giving dteails of the name, type, default value and description. This can then be used by the [fabric8:json maven goal](mavenplugin.html) to list all of the environment variables and their value in the generated kubernetes JSON file.

### Docker

* [docker-api](https://github.com/fabric8io/fabric8/tree/master/components/docker-api) for working directly with a docker host over its REST API

### Etcd

* [etcd-api](https://github.com/fabric8io/fabric8/blob/master/components/fabric-etcd/) provides a Java API for working with [etcd](https://github.com/coreos/etcd/blob/master/README.md)

### Git Repos

* [gitrepo-api](https://github.com/fabric8io/fabric8/blob/master/components/gitrepo-etcd/) provides a Java API for working with git repositories such as  <a href="http://gogs.io/">gogs</a> or <a href="http://github.com/">github</a>

### ActiveMQ

* [mq-client](https://github.com/fabric8io/fabric8/tree/master/components/mq/mq-client) provides the the **io.fabric8.mq.core.MQConnectionFactory** class which implements the JMS ConnectionFactory to connect to ActiveMQ using the [Kubernetes Service](http://fabric8.io/v2/services.html) discovery mechanism which requires no user configuration (other than a single environment variable if you wish to switch to a non default service implementation)

* [camel-amq](https://github.com/fabric8io/fabric8/tree/master/components/mq/camel-amq) provides the [Camel](http://camel.apache.org/) **amq:** component which uses the [Kubernetes Service](http://fabric8.io/v2/services.html) discovery mechanism to discover and connect to the ActiveMQ brokers so that no configuration is required (other than a single environment variable if you wish to switch to a non default service implementation)




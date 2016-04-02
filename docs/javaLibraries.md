## Java Libraries

If you want to write any Java/JVM based tools to interact with [Kubernetes](http://kubernetes.io), [Docker](http://www.docker.com/) or [Etcd](https://github.com/coreos/etcd/blob/master/README.md) we have a number of libraries to help:

### Kubernetes

Kubernetes provides the main REST API for working with the [Kubernetes Platform](http://kubernetes.io). It should provide all you need for writing most services and plugins for Kubernetes.

* [kubernetes-api](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-api) provides a Java API for working with the Kubernetes REST API (pods, replication controllers, services etc)
* [kubernetes-jolokia](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-jolokia) makes it easy to work with the [Jolokia Client API](http://jolokia.org/reference/html/clients.html#client-java) and Java containers running in [Pods](pods.html) inside Kubernetes which expose the Jolokia port
* [kubernetes-template](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-template) provides a simple templating mechanism for generating the Kubernetes JSON files from MVEL templates with parameters from a DTO

#### Testing

* [fabric8-arquillian](https://github.com/fabric8io/fabric8/tree/master/components/fabric8-arquillian) provides a plugin for [Arquillian](fabric8-arquillian) for integration testing [Apps](apps.html) on top of Kubernetes; using Kubernetes to provision and orchestrate the containers and then making [assertions](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-assertions) that the required resources startup correctly.
* [fabric8-selenium](https://github.com/fabric8io/fabric8/tree/master/components/fabric8-selenium) provides a library to make it easier to create [Selenium WebDriver based](http://www.seleniumhq.org/projects/webdriver/) integration and system tests on Kubernetes using [fabric8-arquillian](https://github.com/fabric8io/fabric8/tree/master/components/fabric8-arquillian)
* [kubernetes-assertions](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-assertions) provides a set of [assertj](http://joel-costigliola.github.io/assertj/) assertions of the form **assertThat(kubernetesResource)** for working with the [kubernetes-api](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-api)
* [jolokia-assertions](https://github.com/fabric8io/fabric8/tree/master/components/jolokia-assertions) makes it easy to perform assertions on remote JVMs via JMX using  [Jolokia](http://jolokia.org/) over HTTP/JSON


### ActiveMQ

* [mq-client](https://github.com/fabric8io/fabric8-ipaas/tree/master/mq-client) provides the the **io.fabric8.mq.core.MQConnectionFactory** class which implements the JMS ConnectionFactory to connect to Apache ActiveMQ Artemis using the [Kubernetes Service](http://fabric8.io/guide/services.html) discovery mechanism which requires no user configuration (other than a single environment variable if you wish to switch to a non default service implementation)

* [camel-amq](https://github.com/fabric8io/fabric8-ipaas/tree/master/camel-amq) provides the [Camel](http://camel.apache.org/) **amq:** component which uses the [Kubernetes Service](http://fabric8.io/guide/services.html) discovery mechanism to discover and connect to the ActiveMQ Artemis brokers so that no configuration is required (other than a single environment variable if you wish to switch to a non default service implementation)

### CDI

* [fabric8-cdi](cdi.html) provides an easy way to work with Kubernetes [services](service.html) using the CDI Dependency Injection approach
* [fabric8-apt](https://github.com/fabric8io/fabric8/tree/master/fabric8-apt) provides an APT code generator to create a JSON Schema file for each environment variable injected by the [@ConfigProperty](http://deltaspike.apache.org/documentation/configuration.html) annotation from [deltaspike](http://deltaspike.apache.org/) - giving dteails of the name, type, default value and description. This can then be used by the [fabric8:json maven goal](mavenplugin.html) to list all of the environment variables and their value in the generated kubernetes JSON file.

### DevOps

* [fabric8-devops](https://github.com/fabric8io/fabric8/tree/master/components/fabric8-devops) provides a Java API for reading and writing the `fabric8.yml` file used to configure DevOps options inside the source code of a project

### DevOps Connector

* [fabric8-devops-connector](https://github.com/fabric8io/fabric8/tree/master/components/fabric8-devops-connector) provides a Java library for connecting the various DevOps services like git hosting, chat, issue tracking and jenkins for a project reusing the optional `fabric8.yml` file

### Etcd

* [etcd-api](https://github.com/fabric8io/fabric8/blob/master/components/fabric-etcd/) provides a Java API for working with [etcd](https://github.com/coreos/etcd/blob/master/README.md)

### Git Repos

* [gitrepo-api](https://github.com/fabric8io/fabric8/blob/master/components/gitrepo-api/) provides a Java API for working with git repositories such as  <a href="http://gogs.io/">gogs</a> or <a href="http://github.com/">github</a>

### Hubot

* [hubot-api](https://github.com/fabric8io/fabric8/blob/master/components/hubot-api/) provides a Java API for working with the <a href="http://hubot.github.com/">Hubot chat bot</a> for sending notifications to chat services like Lets Chat, IRC, Slack, HipChat and Campfire

### Hubot

* [hubot-letschat](https://github.com/fabric8io/fabric8/blob/master/components/letschat-api/) provides a Java API for working with the <a href="http://sdelements.github.io/lets-chat/">Let's Chat</a> to auto-create rooms etc.

### Taiga

* [taiga-api](https://github.com/fabric8io/fabric8/blob/master/components/taiga-api/) provides a Java API for working with the <a href="http://taiga.io/">Taiga</a> issue tracker / kanban / scrum management system



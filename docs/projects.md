## Projects

Fabric8 is made up of a number of different modular open source repositories:

### Core

* [kubernetes-model](https://github.com/fabric8io/kubernetes-model) the Java DTOs for working with [kubernetes](http://kubernetes.io/) which are generated from the go source code in kubernetes
* [kubernetes-client](https://github.com/fabric8io/kubernetes-client) the minimal [Kubernetes Java client](https://github.com/fabric8io/kubernetes-client/blob/master/README.md) for working with the [kubernetes REST API](http://kubernetes.io/)
* [fabric8](https://github.com/fabric8io/fabric8) the main java libraries

### DevOps

The following projects are for [Microservices Platform](http://fabric8.io/guide/fabric8DevOps.html):

* [fabric8-devops](https://github.com/fabric8io/fabric8-devops) contains the main [Microservices Platform](http://fabric8.io/guide/fabric8DevOps.html) applications
* [fabric8-console](https://github.com/fabric8io/fabric8-console) the web console for fabric8
* [fabric8-forge](https://github.com/fabric8io/fabric8-forge) contains the main [JBoss Forge addons and REST service](http://fabric8.io/guide/forge.html) 
* [jenkins-workflow-library](https://github.com/fabric8io/jenkins-workflow-library) the reusable [Jenkins Workflow library](http://fabric8.io/guide/jenkinsWorkflowLibrary.html)
* [fabric8-jenkins-workflow-steps](https://github.com/fabric8io/fabric8-jenkins-workflow-steps) the Jenkins Pipeline steps for working with [Microservices Platform](http://fabric8.io/guide/fabric8DevOps.html)
* [kubernetes-workflow](https://github.com/fabric8io/kubernetes-workflow) extends Jenkins Pipeline to have native Kubernetes support for creating build slaves using custom pods, images, volumes and secrets

### iPaaS

The following projects are for [Fabric8 iPaaS](http://fabric8.io/guide/ipaas.html):

* [fabric8-ipaas](https://github.com/fabric8io/fabric8-ipaas) contains the main [iPaaS](http://fabric8.io/guide/ipaas.html) applications
* [ipaas-quickstarts](https://github.com/fabric8io/ipaas-quickstarts) contains the [quickstarts](http://fabric8.io/guide/quickstarts/index.html) and [archetypes](http://fabric8.io/guide/quickstarts/archetypes.html) for the [iPaaS](http://fabric8.io/guide/ipaas.html)

### Tools

* [gofabric8](https://github.com/fabric8io/gofabric8) is a go based CLI tool for deploying fabric8
* [helm](https://github.com/fabric8io/helm) is a fork of [helm](http://helm.sh/) for working with OpenShift and Fabric8

### Suppport for non-docker

Some folks have work loads they need to orchestrate on operating systems that don't yet have production quality docker support (e.g. Windows, AIX, Solaris, HPUX).

* [kansible](https://github.com/fabric8io/kansible) lets you orchestrate operating system processes on Windows or any Unix in the same way as you orchestrate your Docker containers with Kubernetes by using Ansible to provision the software onto hosts and Kubernetes to orchestrate the processes and the containers in a single system

### Additional projects

The web console uses many different [hawtio 2 modules](http://hawt.io/overview/index.html). In particular the main dependency of is [hawtio-kubernetes](https://github.com/hawtio/hawtio-kubernetes)

### Docker images

There are numerous docker images created via separate github repositories such as the following:

* [docker-gerrit](https://github.com/fabric8io/docker-gerrit)
* [docker-grafana](https://github.com/fabric8io/docker-grafana)
* [docker-gogs](https://github.com/fabric8io/docker-gogs)
* [docker-prometheus](https://github.com/fabric8io/docker-prometheus)
* [nexus-docker](https://github.com/fabric8io/nexus-docker)
* [hubot-irc](https://github.com/fabric8io/hubot-irc)
* [hubot-lets-chat](https://github.com/fabric8io/hubot-lets-chat)
* [hubot-slack](https://github.com/fabric8io/hubot-slack)
* [fabric8-eclipse-orion](https://github.com/fabric8io/fabric8-eclipse-orion)
* [fabric8-kiwiirc](https://github.com/fabric8io/fabric8-kiwiirc)
* [jenkins-docker](https://github.com/fabric8io/jenkins-docker)
* [lets-chat](https://github.com/fabric8io/lets-chat)
* [taiga-docker](https://github.com/fabric8io/taiga-docker)
* [openshift-auth-proxy](https://github.com/fabric8io/openshift-auth-proxy)

### Base images

The above-packaged docker images leverage some of these base Docker images:

#### Java Alpine Linux
* [docker.io/fabric8/java-alpine-openjdk8-jdk](https://github.com/fabric8io/base-images/tree/master/java/images/alpine/openjdk8/jdk)
* [docker.io/fabric8/java-alpine-openjdk8-jre](https://github.com/fabric8io/base-images/tree/master/java/images/alpine/openjdk8/jre)
* [docker.io/fabric8/java-alpine-openjdk7-jdk](https://github.com/fabric8io/base-images/tree/master/java/images/alpine/openjdk7/jdk)
* [docker.io/fabric8/java-alpine-openjdk7-jre](https://github.com/fabric8io/base-images/tree/master/java/images/alpine/openjdk7/jdk)

#### Java Centos Linux
* [docker.io/fabric8/java-centos-openjdk8-jdk](https://github.com/fabric8io/base-images/tree/master/java/images/centos/openjdk8/jdk)
* [docker.io/fabric8/java-centos-openjdk8-jre](https://github.com/fabric8io/base-images/tree/master/java/images/centos/openjdk8/jre)
* [docker.io/fabric8/java-centos-openjdk7-jdk](https://github.com/fabric8io/base-images/tree/master/java/images/centos/openjdk7/jdk)
* [docker.io/fabric8/java-centos-openjdk7-jre](https://github.com/fabric8io/base-images/tree/master/java/images/centos/openjdk7/jdk)

#### JBoss
* [docker.io/fabric8/java-jboss-openjdk8-jdk](https://github.com/fabric8io/base-images/tree/master/java/images/jboss/openjdk8/jdk)


#### Jetty
* [docker.io/fabric8/jetty-9](https://github.com/fabric8io/base-images/tree/master/jetty/images/9)
* [docker.io/fabric8/jetty-8](https://github.com/fabric8io/base-images/tree/master/jetty/images/8)
 
#### Karaf
* [docker.io/fabric8/karaf-2.4](https://github.com/fabric8io/base-images/tree/master/karaf/images/2.4)
* [docker.io/fabric8/karaf-3.0](https://github.com/fabric8io/base-images/tree/master/karaf/images/3)

#### Tomcat
* [docker.io/fabric8/tomcat-8.0](https://github.com/fabric8io/base-images/tree/master/tomcat/images/8)
* [docker.io/fabric8/tomcat-7.0](https://github.com/fabric8io/base-images/tree/master/tomcat/images/7)

#### s2i
* [docker.io/fabric8/s2i-java](https://github.com/fabric8io-images/s2i/tree/master/java)
* [docker.io/fabric8/s2i-karaf](https://github.com/fabric8io-images/s2i/tree/master/karaf)



## Tools

Fabric8 provides many tools to help you work with [Kubernetes](http://kubernetes.io) and [OpenShift](http://www.openshift.org/)

* [Maven Plugin](mavenPlugin.html) helps you work with fabric8 and kubernetes from inside maven, your builds and releases
  * [docker:build](mavenDockerBuild.html) builds the docker image for your maven project
  * [docker:push](mavenDockerPush.html) pushes the locally built docker image to the global or a local docker registry
  * [fabric8:json](mavenFabric8Json.html) generates kubernetes json for your maven project
  * [fabric8:apply](mavenFabric8Apply.html) applies the kubernetes json into a namespace in a kubernetes cluster
* [Java Libraries](javaLibraries.html) provides a number of Java libraries for working with and testing Kubernetes, Docker, etcd, ActiveMQ etc.
* [CDI](cdi.html) provides an easy way to work with Kubernetes [services](service.html) using the CDI Dependency Injection approach
* [Testing](testing.html) helps you perform integration and system tests of your [apps](apps.html) on top of Kubernetes
* [Forge Addons](forge.html) provides a universal command line shell and IDE plugins for working with your projects and fabric8
* [QuickStarts](quickstarts/index.html) provide a really easy way to get started


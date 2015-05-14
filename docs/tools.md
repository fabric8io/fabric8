## Tools

There are various tools to help you work with fabric8:

* [Maven Plugin](mavenPlugin.html) helps you work with fabric8 and kubernetes from inside maven, your builds and releases
  * [docker:build](mavenDockerBuild.html) builds the docker image for your maven project
  * [docker:push](mavenDockerPush.html) pushes the locally built docker image to the global or a local docker registry
  * [fabric8:json](mavenFabric8Json.html) generates kubernetes json for your maven project
  * [fabric8:apply](mavenFabric8Apply.html) applies the kubernetes json into a namespace in a kubernetes cluster
* [Java Libraries](javaLibraries.html) provides a number of Java libraries for working with and testing Kubernetes, Docker, etcd, ActiveMQ etc.
* [CDI](cdi.html) provides an easy way to work with Kubernetes [services](service.html) using the CDI Dependency Injection approach
* [Testing](testing.html) helps you perform integration tests of your [apps](apps.html)
* [QuickStarts](quickstarts.html) provide a really easy way to get started
* [Forge Addons](forge.html) provides a universal command line shell for working with your projects and fabric8

There are also a number of [fabric8 apps](fabric8Apps.html) you can run on any Kubernetes environment.
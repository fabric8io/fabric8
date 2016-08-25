## Developer workflow

If you tend to use an IDE for working with Java and things like [Apache Camel](http://camel.apache.org) and [Apache CXF](http://cxf.apache.org/) here's how to get started using your IDE or [Maven](http://maven.apache.org/) with [fabric8](http://fabric8.io/).

There's also a [more in depth screencast](http://www.christianposta.com/blog/?p=373) if you prefer to watch that first.

### To start create an archetype

We've got lots of [quickstarts](https://github.com/fabric8-quickstarts) which we've converted into [Maven Archetypes](https://maven.apache.org/guides/introduction/introduction-to-archetypes.html).

So if you have [installed Maven](http://maven.apache.org/download.cgi#Installation), type the following:

    mvn org.apache.maven.plugins:maven-archetype-plugin:2.2:generate -Dfilter=io.fabric8:

This will list all the various archetypes. Pick one that suits your fancy, e.g. **io.fabric8.archetypes:springboot-camel-archetype**
    mvn org.apache.maven.plugins:maven-archetype-plugin:2.2:generate -Dfilter=io.fabric8:spring-boot-camel-archetype

for the [Camel Spring Boot Quickstart](https://github.com/fabric8-quickstarts/spring-boot-camel).

Then enter these values:

    groupId:    cool
    artifactId: mydemo
    version:    1.0.0.SNAPSHOT
    package:    cool

And confirm with 'Y'.

Now change directory in to the project

    cd mydemo

build the java quickstart, generate the Kubernetes resources, build the docker image and deploy it to Kubernetes

    mvn fabric8:run

or if not working on minikube and deploying to a cluster

    mvn fabric8:run fabri8:push -Ddocker.push.registry=my.registry.io

Now check your pods and wait for your cool demo pod to start

    kubectl get pods -w

Fabric8 Docker API
==================

This bundle creates a JAXRS 2.0 based Java interface API to [Docker](http://docker.io/)

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

             <dependency>
                 <groupId>io.fabric8</groupId>
                 <artifactId>docker-api</artifactId>
                 <version>2.2.35</version>
             </dependency>

### Try an example

If you clone the source code:

    git clone https://github.com/fabric8io/fabric8.git
    cd fabric8

Install docker so that **$DOCKER_HOST** is pointing to the docker host REST API then run

    cd components/docker-api
    mvn clean test-compile exec:java

The Example program should start, list some containers, create a container and so forth.

### API Overview

You use the **DockerFactory** to create an instance of [Docker](https://github.com/fabric8io/fabric8/blob/master/components/docker-api/src/main/java/io/fabric8/docker/api/Docker.java#L46) which supports the [Docker Remote API](http://docs.docker.io/en/latest/reference/api/docker_remote_api/)

For example:

    DockerFactory dockerFactory = new DockerFactory();
    Docker docker = dockerFactory.createDocker();
    List<Container> containers = docker.containers(1, 10, null, null, 1);

To see more of the [Docker API](https://github.com/fabric8io/fabric8/blob/master/components/docker-api/src/main/java/io/fabric8/docker/api/Docker.java#L47) in action [check out this example](https://github.com/fabric8io/fabric8/blob/master/components/docker-api/src/test/java/io/fabric8/docker/api/Example.java#L64)

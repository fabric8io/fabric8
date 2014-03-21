Fabric8 Docker API
==================

This bundle creates a JAXRS 2.0 based Java interface API to [Docker](http://docker.io/)

To try this out, install docker so that $DOCKER_HOST is pointing to the host (or it defaults to localhost) then run

    mvn clean test-compile exec:java

The Example program should start, list some containers, create a container and so forth.

### API Overview

You use the **DockerFactory** to create an instance of [Docker](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric-docker-api/src/main/java/io/fabric8/docker/api/Docker.java#L46) which supports the [Docker Remote API](http://docs.docker.io/en/latest/reference/api/docker_remote_api/)

For example:

    DockerFactory dockerFactory = new DockerFactory();
    Docker docker = dockerFactory.createDocker();
    List<Container> containers = docker.containers(1, 10, null, null, 1);

To see more of the [Docker API](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric-docker-api/src/main/java/io/fabric8/docker/api/Docker.java#L46) in action [check out this example](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric-docker-api/src/test/java/io/fabric8/docker/api/Example.java#L54)
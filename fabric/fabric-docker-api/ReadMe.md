Fabric8 Docker API
==================

This bundle creates a JAXRS 2.0 based Java interface API to [Docker](http://docker.io/)

To try this out, install docker so that $DOCKER_HOST is pointing to the host (or it defaults to localhost) then run

    mvn clean test-compile exec:java

The Example program should start, list some containers, create a container and so forth.
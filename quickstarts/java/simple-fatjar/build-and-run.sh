#!/bin/bash
mvn clean install docker:build
docker push $DOCKER_REGISTRY/quickstart/java-simple-fatjar:2.0.0-SNAPSHOT
mvn fabric8:run

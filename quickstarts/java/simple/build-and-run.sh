#!/bin/bash
mvn clean install docker:build
docker push $DOCKER_REGISTRY/quickstarts/java-simple:2.0.0-SNAPSHOT
mvn fabric8:deploy

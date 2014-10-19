#!/bin/bash
mvn clean install docker:build
docker push $DOCKER_REGISTRY/quickstart/springboot-webmvc:2.0.0-SNAPSHOT
mvn fabric8:run

#!/bin/bash
mvn clean install docker:build
docker push $DOCKER_REGISTRY/fabric8/fabric8-mq-consumer:2.0.0-SNAPSHOT
mvn clean fabric8:json
mvn fabric8:run

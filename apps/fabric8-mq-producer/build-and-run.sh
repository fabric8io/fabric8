#!/bin/bash
mvn clean install docker:build
docker push $DOCKER_REGISTRY/fabric8/fabric8-mq-producer:2.0.0-SNAPSHOT
mvn fabric8:deploy

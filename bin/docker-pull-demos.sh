#!/bin/bash

export FABRIC8_VERSION=2.0.10

echo "pulling demo images...."

docker pull fabric8/fabric8-mq:$FABRIC8_VERSION
docker pull fabric8/fabric8-mq-producer:$FABRIC8_VERSION
docker pull fabric8/fabric8-mq-consumer:$FABRIC8_VERSION
docker pull fabric8/api-registry:$FABRIC8_VERSION
docker pull fabric8/quickstart-java-cxf-cdi:$FABRIC8_VERSION

echo "demos pulled!"

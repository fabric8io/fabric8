#!/bin/bash -e

export DOCKER_IP=127.0.0.1
export DOCKER_REGISTRY=$DOCKER_IP:5000
export KUBERNETES_MASTER=http://$DOCKER_IP:8080
export FABRIC8_CONSOLE=http://$DOCKER_IP:8484/hawtio

docker pull openshift/origin:latest
docker run -d --name=openshift -v /var/run/docker.sock:/var/run/docker.sock -v `pwd`:/manifests --privileged --net=host openshift/origin:latest start

./build-latest-cadvisor.sh

docker run -d --name=cadvisor -p 4194:8080 \
  --volume=/:/rootfs:ro \
  --volume=/var/run:/var/run:rw \
  --volume=/sys:/sys:ro \
  --volume=/var/lib/docker/:/var/lib/docker:ro \
  google/cadvisor:canary

docker exec openshift openshift kube apply -c /manifests/registry.json
docker exec openshift openshift kube apply -c /manifests/influxdb.json
docker exec openshift openshift kube apply -c /manifests/fabric8.json

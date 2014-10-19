#!/bin/sh

export OPENSHIFT_HOST=172.16.123.2

if grep -Fxq "# Openshift automated script start" /etc/hosts
then
   echo "Environment already setup, remove OpenShift entries from /etc/hosts to setup again"
else
   echo "Setting up environment"
   export OPENSHIFT_HOST=172.16.123.2
   sudo ifconfig lo0 alias $OPENSHIFT_HOST
   echo "Updating /etc/hosts with openshift alias"
   echo "# Openshift automated script start" | sudo tee -a /etc/hosts
   echo $OPENSHIFT_HOST openshifthost  | sudo tee -a /etc/hosts
   echo `boot2docker ip 2> /dev/null` dockerhost | sudo tee -a /etc/hosts
   echo "# Openshift automated script end" | sudo tee -a /etc/hosts

   export KUBERNETES_MASTER=http://openshifthost:8080
   export DOCKER_REGISTRY=dockerhost:5000
   export DOCKER_HOST=tcp://dockerhost:2375
fi

echo "KUBERNETES_MASTER" $KUBERNETES_MASTER
echo "DOCKER_REGISTRY" $DOCKER_REGISTRY
echo "DOCKER_HOST" $DOCKER_HOST
echo "OPENSHIFT_HOST" $OPENSHIFT_HOST

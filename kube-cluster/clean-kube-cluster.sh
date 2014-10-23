#!/bin/bash

./kubernetes/cluster/kubecfg.sh delete services/registry-mirror-service
./kubernetes/cluster/kubecfg.sh delete services/registry-service
./kubernetes/cluster/kubecfg.sh delete services/hawtio-service

./kubernetes/cluster/kubecfg.sh delete replicationControllers/registryMirrorController
./kubernetes/cluster/kubecfg.sh delete replicationControllers/registryController
./kubernetes/cluster/kubecfg.sh delete replicationControllers/hawtioController

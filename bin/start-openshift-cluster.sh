#!/bin/bash

OPENSHIFT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
OPENSHIFT_SOURCE_DIR=${OPENSHIFT_DIR}/openshift-cluster

cd ${OPENSHIFT_DIR}

OPENSHIFT_DOWNLOAD_URL=https://github.com/openshift/origin
if [ ! -d "${OPENSHIFT_SOURCE_DIR}" ]; then
  git clone ${OPENSHIFT_DOWNLOAD_URL} ${OPENSHIFT_SOURCE_DIR}
fi

cd ${OPENSHIFT_SOURCE_DIR}
git pull

if [ ! -f .vagrant-openshift.json ]; then
  echo '{"dev_cluster": true}' >  .vagrant-openshift.json
fi

vagrant up

grep master /etc/hosts || echo '10.245.1.2 master' | sudo tee -a /etc/hosts
grep minion-1 /etc/hosts || echo '10.245.1.3 minion-1' |sudo tee -a /etc/hosts
grep minion-2 /etc/hosts || echo '10.245.1.4 minion-2' | sudo tee -a /etc/hosts

./_output/local/bin/linux/amd64/openshift kube apply -c ../influxdb.json

echo "Starting cadvisor on each minion"
for m in minion-1 minion-2; do
  vagrant ssh $m -- docker run -d --name=cadvisor -p 8080:8080 \
    --volume=/:/rootfs:ro \
    --volume=/var/run:/var/run:rw \
    --volume=/sys:/sys:ro \
    --volume=/var/lib/docker/:/var/lib/docker:ro \
    google/cadvisor:latest \
    -storage_driver=influxdb \
    -storage_driver_host=172.121.17.1:8086
done

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

vagrant ssh minion-1 -- sudo ip route add 10.244.2.0/24 via 10.245.2.3 dev enp0s8
vagrant ssh minion-2 -- sudo ip route add 10.244.1.0/24 via 10.245.2.2 dev enp0s8

grep master /etc/hosts || echo '10.245.1.2 master' | sudo tee -a /etc/hosts
grep minion-1 /etc/hosts || echo '10.245.1.3 minion-1' |sudo tee -a /etc/hosts
grep minion-2 /etc/hosts || echo '10.245.1.4 minion-2' | sudo tee -a /etc/hosts

cp ../build-latest-cadvisor.sh .

vagrant ssh minion-1 -- /vagrant/build-latest-cadvisor.sh
vagrant ssh minion-1 -- docker save -o /vagrant/google-cadvisor-image.tar google/cadvisor:canary
vagrant ssh minion-2 -- docker load -i /vagrant/google-cadvisor-image.tar

for m in minion-1 minion-2; do
  vagrant ssh $m -- docker run -d --name=cadvisor -p 4194:8080 \
    --volume=/:/rootfs:ro \
    --volume=/var/run:/var/run:rw \
    --volume=/sys:/sys:ro \
    --volume=/var/lib/docker/:/var/lib/docker:ro \
    google/cadvisor:canary
done

export KUBERNETES_MASTER=http://master:8080
./_output/local/bin/linux/amd64/openshift kube apply -c ../influxdb.json

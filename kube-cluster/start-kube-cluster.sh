#!/bin/bash

KUBE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
KUBE_VERSION=${KUBE_VERSION:-0.4.1}

cd ${KUBE_DIR}

if [ ! -d "${KUBE_DIR}/kubernetes" ]; then
  case ${KUBE_VERSION} in
    master)
      KUBE_DOWNLOAD_URL=https://github.com/GoogleCloudPlatform/kubernetes/archive/master.tar.gz
      ;;
    *)
      KUBE_DOWNLOAD_URL=https://github.com/GoogleCloudPlatform/kubernetes/releases/download/v${KUBE_VERSION}/kubernetes.tar.gz
      ;;
  esac

  mkdir ${KUBE_DIR}
  curl -L ${KUBE_DOWNLOAD_URL} | tar xvz -C ${KUBE_DIR}
fi

cd ${KUBE_DIR}/kubernetes
export KUBERNETES_PROVIDER=vagrant
export KUBERNETES_MASTER=https://10.245.1.2
./cluster/kube-up.sh

MINION_1_IP=10.245.2.2

${KUBE_DIR}/kubernetes/cluster/kubecfg.sh -c ${KUBE_DIR}/docker-registry-mirror-controller.json create replicationControllers
${KUBE_DIR}/kubernetes/cluster/kubecfg.sh -c ${KUBE_DIR}/docker-registry-mirror-service.json create services

${KUBE_DIR}/kubernetes/cluster/kubecfg.sh -c ${KUBE_DIR}/docker-registry-controller.json create replicationControllers
${KUBE_DIR}/kubernetes/cluster/kubecfg.sh -c ${KUBE_DIR}/docker-registry-service.json create services

${KUBE_DIR}/kubernetes/cluster/kubecfg.sh -c ${KUBE_DIR}/hawtio-controller.json create replicationControllers
${KUBE_DIR}/kubernetes/cluster/kubecfg.sh -c ${KUBE_DIR}/hawtio-service.json create services

while  ! curl -s -m 10  "http://${MINION_1_IP}:5001" > /dev/null;  do echo "DEBUG: Docker registry mirror not running yet." ; sleep 10s; done; echo "INFO: Docker registry mirror ready."

while  ! curl -s -m 10  "http://${MINION_1_IP}:5000" > /dev/null;  do echo "DEBUG: Docker registry not running yet." ; sleep 10s; done; echo "INFO: Docker registry ready."

while  ! curl -s -m 10  "http://${MINION_1_IP}:8080" > /dev/null;  do echo "DEBUG: Fabric8 Hawtio not running yet." ; sleep 10s; done; echo "INFO: Fabric8 Hawtio ready."

echo "Hawtio is available at http://${MINION_1_IP}:8080/hawtio"

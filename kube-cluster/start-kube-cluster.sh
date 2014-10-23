#!/bin/bash

KUBE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
KUBE_VERSION=${KUBE_VERSION:-master}
KUBE_SOURCE_DIR=${KUBE_DIR}/kubernetes

cd ${KUBE_DIR}

case ${KUBE_VERSION} in
  master)
    KUBE_DOWNLOAD_URL=https://github.com/GoogleCloudPlatform/kubernetes.git
    if [ ! -d "${KUBE_SOURCE_DIR}" ]; then
      git clone ${KUBE_DOWNLOAD_URL} ${KUBE_SOURCE_DIR}
    else
      pushd ${KUBE_SOURCE_DIR}
      git pull
      popd
    fi
    echo "You're trying to run master - going to have to build from source"
    echo "This might take a while - not to build, but to download necessary docker image, if you don't already have it"
    pushd ${KUBE_SOURCE_DIR}
    ./build/release.sh
    popd
    ;;
  *)
    KUBE_DOWNLOAD_URL=https://github.com/GoogleCloudPlatform/kubernetes/releases/download/v${KUBE_VERSION}/kubernetes.tar.gz
    if [ ! -d "${KUBE_SOURCE_DIR}" ]; then
      curl -L ${KUBE_DOWNLOAD_URL} | tar xvz -C ${KUBE_DIR}
    fi
    ;;
esac

cd ${KUBE_SOURCE_DIR}
export KUBERNETES_PROVIDER=vagrant
export KUBERNETES_MASTER=https://10.245.1.2
export KUBERNETES_NUM_MINIONS=1
./cluster/kube-up.sh

MINION_1_IP=10.245.2.2

#${KUBE_SOURCE_DIR}/cluster/kubectl.sh create -f ${KUBE_DIR}/docker-registry-mirror-controller.json
#${KUBE_SOURCE_DIR}/cluster/kubectl.sh create -f ${KUBE_DIR}/docker-registry-mirror-service.json

${KUBE_SOURCE_DIR}/cluster/kubectl.sh create -f ${KUBE_DIR}/logspout-controller.json

${KUBE_SOURCE_DIR}/cluster/kubectl.sh create -f ${KUBE_DIR}/docker-registry-controller.json
${KUBE_SOURCE_DIR}/cluster/kubectl.sh create -f ${KUBE_DIR}/docker-registry-service.json

${KUBE_SOURCE_DIR}/cluster/kubectl.sh create -f ${KUBE_DIR}/hawtio-controller.json
${KUBE_SOURCE_DIR}/cluster/kubectl.sh create -f ${KUBE_DIR}/hawtio-service.json

#while  ! curl -s -m 10  "http://${MINION_1_IP}:5001" > /dev/null;  do echo "DEBUG: Docker registry mirror not running yet." ; sleep 10s; done; echo "INFO: Docker registry mirror ready."

while  ! curl -s -m 10  "http://${MINION_1_IP}:5000" > /dev/null;  do echo "DEBUG: Docker registry not running yet." ; sleep 10s; done; echo "INFO: Docker registry ready."

while  ! curl -s -m 10  "http://${MINION_1_IP}:8080" > /dev/null;  do echo "DEBUG: Fabric8 Hawtio not running yet." ; sleep 10s; done; echo "INFO: Fabric8 Hawtio ready."

echo "Kubernetes master is available at ${KUBERNETES_MASTER}"
echo "Docker registry is available at http://${MINION_1_IP}:5000"
#echo "Docker registry Hub mirror is available at http://${MINION_1_IP}:5001"
echo "Hawtio is available at http://${MINION_1_IP}:8080/hawtio"

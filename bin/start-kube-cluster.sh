#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
KUBE_VERSION=${KUBE_VERSION:-0.4.1}

ROOTDIR=${DIR}/../
KUBE_DIR=${ROOTDIR}/kube-cluster/

cd ${ROOTDIR}

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

${KUBE_DIR}/kubernetes/cluster/kubecfg.sh -c ${ROOTDIR}/apps/docker-registry-controller.json create replicationControllers
${KUBE_DIR}/kubernetes/cluster/kubecfg.sh -c ${ROOTDIR}/apps/hawtio-controller.json create replicationControllers

${KUBE_DIR}/kubernetes/cluster/kubecfg.sh -c ${ROOTDIR}/apps/docker-registry-service.json create services
${KUBE_DIR}/kubernetes/cluster/kubecfg.sh -c ${ROOTDIR}/apps/hawtio-service.json create services

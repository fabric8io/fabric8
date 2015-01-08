#!/bin/bash

#
# Discover the APP_BASE from the location of this script.
#
if [ -z "$APP_BASE" ] ; then
  DIRNAME=`dirname "$0"`
  APP_BASE=`cd "$DIRNAME"; pwd`
  export APP_BASE
fi

export VAGRANT_VAGRANTFILE=$APP_BASE/../Vagrantfile-atomic

vagrant up

SERVICE_IP=172.28.128.5
KUBEMASTER_IP=172.28.128.4

function addPublicIP {
  local contents
  while read data; do
    contents="$contents $data"
  done
  cat <<EOF | python -
import json, sys
config=json.loads('$contents')
for obj in config["items"]:
  if obj["kind"] == "Service":
    obj["publicIPs"] = ["$SERVICE_IP"]
print json.dumps(config, indent=2)
EOF
}

if [ -f "$APP_BASE/fabric8.json" ]; then
  cat $APP_BASE/fabric8.json | addPublicIP | VAGRANT_VAGRANTFILE=../Vagrantfile-atomic vagrant ssh -c "sudo docker run --rm -i openshift/origin:latest cli -shttp://$KUBEMASTER_IP:8080 apply -f -"
fi

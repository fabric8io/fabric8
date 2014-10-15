#!/bin/bash
# configure shell log
export PS4='+(${BASH_SOURCE}:${LINENO}): ${FUNCNAME[0]:+${FUNCNAME[0]}(): }'

set -e

# if OPENSHIFT_IP is not set, try figure out a good IP address to use - maybe checking if it can be pinged from docker.
if [ -z "$OPENSHIFT_IP" ]; then
    export OPENSHIFT_IP=$(ip a s docker0 | grep 'inet ' | awk '{print $2}' | cut -f1 -d'/')
    echo "OPENSHIFT_IP env var not set. Assigning docker0 ip to it: $OPENSHIFT_IP"

fi  

if [ -z "$DOCKER_IP" ]; then
    export DOCKER_IP=$(ip a s docker0 | grep 'inet ' | awk '{print $2}' | cut -f1 -d'/')
    echo "DOCKER_IP env var not set. Assigning docker0 ip to it: $DOCKER_IP"
fi  

export KUBERNETES_MASTER="http://$OPENSHIFT_IP:8080"
export DOCKER_REGISTRY="$DOCKER_IP:5000"
export DOCKER_HOST="tcp://$DOCKER_IP:2375"

sed 's/dockerhost/'"$DOCKER_IP"'/g' fabric8.json > modifiedFabric8.json
sed -i 's/openshifthost/'"$OPENSHIFT_IP"'/g' modifiedFabric8.json

# run openshift 
openshift start --listenAddr="$OPENSHIFT_IP:8080" > openshift.log 2> openshift.log & 

while  ! curl -m 10  "http://$OPENSHIFT_IP:8080";  do echo "DEBUG: Openshift REST layer not running yet." ; sleep 2s; done; echo "INFO: Openshift REST layer ready."

echo "INFO Checking communications from Docker Containers to Host"
docker run --rm -it base bash -c "echo probe > /dev/tcp/$OPENSHIFT_IP/8080"

if [[ $?  != 0 ]] ; then
  echo "ERROR: Docker containers have problems to communicate with Openshift Rest API"
  echo "INFO: Checking firewall state:"
  FIREWALL_STATUS=$(systemctl is-active firewalld)
  if [[ $FIREWALL_STATUS == "active" ]]; then
  	echo "WARN: Your firewall is active. This might interfere with the communication. You might consider to stop the service and restart Docker daemon"
  	echo "WARN: sudo systemctl firewalld stop"
  	echo "WARN: sudo systemctl docker restart"
  else
  	echo "INFO: No Firewall blocking the communication from Container to Host"
  fi
  echo "INFO: Checking Docker daemon configuration:"
  DOCKER_DAEMON=$(systemctl status docker | grep bin/docker)
  if [[ "$DOCKER_DAEMON" =~ -H[:blank:]*$OPENSHIFT_IP ]] || [[ "$DOCKER_DAEMON" =~ -H[:blank:]*0\.0\.0\.0 ]]; then
  	echo "ERROR: Your Docker Daemon is not listening on the OpenShift assigned IP address"
  else
  	echo "INFO: Your Docker Daemon is listening on the correct IP address: $OPENSHIFT_IP"
  fi
  echo "INFO: If previous checks passed, your iptables configuration could be blocking the traffic."
  echo "INFO: You migh want to add a line like:"
  echo "       sudo iptables -I INPUT -i docker0 -p tcp -j ACCEPT"
  echo "       and/or"
  echo "       sudo iptables -I FORWARD -i docker0 -o docker0 -j ACCEPT"
  exit 1
else
  echo "INFO: Automated checks PASSED"
  echo "INFO: Provisioning Fabric8"
  openshift kube apply -c modifiedFabric8.json 
  echo "INFO: Tailing openshift.log..."
  tail -F -n 2000 openshift.log
fi



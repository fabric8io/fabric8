#!/bin/bash

#
# Discover the APP_BASE from the location of this script.
#
if [ -z "$APP_BASE" ] ; then
  DIRNAME=`dirname "$0"`
  APP_BASE=`cd "$DIRNAME"; pwd`
  export APP_BASE
fi

echo "Validating your environment..."
echo

for image in google/cadvisor:latest openshift/origin:latest openshift/origin-haproxy-router:latest registry:latest tutum/influxdb:latest fabric8/hawtio:latest kubernetes/fluentd-elasticsearch:latest; do
  (
    IFS=':' read -a splitimage <<< "$image"
    docker images | grep -qEo "${splitimage[0]}\W+${splitimage[1]}" || (echo "Missing necessary Docker image: $image" && docker pull $image && echo)
  )
done

echo "Validating firewall rules"
RULE="INPUT -d 172.17.42.1 -s 172.17.0.0/16 -j ACCEPT"
RULE_OUTPUT=$( { docker run --rm --privileged --net=host google/cadvisor:latest iptables -C $RULE; } 2>/dev/null )
test -n "$RULE_OUTPUT" && echo "Inserting firewall rule to allow containers to communicate to Docker daemon" && docker run --rm --privileged --net=host google/cadvisor:latest -I $RULE
RULE="INPUT -d 172.17.0.0/16 -s 172.121.0.0/16 -j ACCEPT"
RULE_OUTPUT=$( { docker run --rm --privileged --net=host google/cadvisor:latest iptables -C $RULE; } 2>/dev/null )
test -n "$RULE_OUTPUT" && echo "Inserting firewall rule to allow containers to communicate with Kubernetes services" && docker run --rm --privileged --net=host google/cadvisor:latest iptables -I $RULE
RULE="INPUT -d 172.121.0.0/16 -s 172.17.0.0/16 -j ACCEPT"
RULE_OUTPUT=$( { docker run --rm --privileged --net=host google/cadvisor:latest iptables -C $RULE; } 2>/dev/null )
test -n "$RULE_OUTPUT" && docker run --rm --privileged --net=host google/cadvisor:latest iptables -I $RULE
echo

while getopts "fud:" opt; do
  case $opt in
    f)
      echo "Cleaning up all existing k8s containers"
      docker rm -f openshift cadvisor || true
      RUNNING_CONTAINERS=`docker ps -a | grep k8s | cut -c 1-12`
      test -z "$RUNNING_CONTAINERS" || docker rm -f $RUNNING_CONTAINERS
      echo
      ;;
    u)
      echo "Updating all necessary images"
      for image in google/cadvisor:latest openshift/origin:latest openshift/origin-haproxy-router:latest registry:latest tutum/influxdb:latest fabric8/hawtio:latest kubernetes/fluentd-elasticsearch:latest; do
        docker pull $image
      done
      echo
      ;;
    d)
      DOCKER_IP=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      echo
      ;;
  esac
done


# TODO it would be nice if we could tell easily if these routes have already been applied so we don't have to do this each time
if [[ $OSTYPE == darwin* ]]; then
    if [ -z "$DOCKER_IP" ] ; then
      export DOCKER_IP=`boot2docker ip 2> /dev/null`
    fi

    echo "Adding network routes to 172.17.0.0/24 & 172.121.17.0/24 via $DOCKER_IP so that the host operating system can see pods and services inside OpenShift"
    sudo route -n add 172.17.0.0/24 $DOCKER_IP
    sudo route -n add 172.121.17.0/24 $DOCKER_IP
fi

export DOCKER_IP=${DOCKER_IP:-127.0.0.1}
export DOCKER_REGISTRY=$DOCKER_IP:5000
export KUBERNETES_MASTER=http://$DOCKER_IP:8080
export FABRIC8_CONSOLE=http://$DOCKER_IP:8484/hawtio


# using an env var but ideally we'd use an alias ;)
KUBE="docker run --rm -i --net=host openshift/origin:latest kube"

OPENSHIFT_CONTAINER=$(docker run -d --name=openshift -v /var/run/docker.sock:/var/run/docker.sock --privileged --net=host openshift/origin:latest start)

# Have to run it privileged otherwise not working on CentOS7
CADVISOR_CONTAINER=$(docker run -d --name=cadvisor --privileged -p 4194:8080 \
  --volume=/:/rootfs:ro \
  --volume=/var/run:/var/run:rw \
  --volume=/sys:/sys:ro \
  --volume=/var/lib/docker/:/var/lib/docker:ro \
  google/cadvisor:latest)

if [ -f "$APP_BASE/registry.json" ]; then
  cat $APP_BASE/fabric8.json | $KUBE apply -c -
  cat $APP_BASE/registry.json | $KUBE apply -c -
  cat $APP_BASE/influxdb.json | $KUBE apply -c -
  cat $APP_BASE/elasticsearch.json | $KUBE apply -c -
  cat $APP_BASE/fluentd.yml | $KUBE apply -c -
  cat $APP_BASE/kibana.yml | $KUBE apply -c -
  cat $APP_BASE/router.json | $KUBE create pods -c -
else
  $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/fabric8.json
  $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/registry.json
  $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/influxdb.json
  $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/elasticsearch.json
  $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/fluentd.yml
  $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/kibana.yml
  $KUBE create pods -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/router.json
fi

K8S_SERVICES=$($KUBE list services)

echo
echo "Waiting for services to fully come up - shouldn't be too long for you to wait"
echo

getServiceIpAndPort()
{
  echo `echo "$1"|grep $2| sed 's/\s\+/ /g' | awk '{ print $3 ":" $4 }'`
}

FABRIC8_CONSOLE=http://$(getServiceIpAndPort "$K8S_SERVICES" hawtio-service)/hawtio/
DOCKER_REGISTRY=http://$(getServiceIpAndPort "$K8S_SERVICES" registry)
INFLUXDB=http://$(getServiceIpAndPort "$K8S_SERVICES" influx-master)
ELASTICSEARCH=http://$(getServiceIpAndPort "$K8S_SERVICES" elasticsearch)
KIBANA_CONSOLE=http://$(getServiceIpAndPort "$K8S_SERVICES" kibana-service)
KUBERNETES=http://$DOCKER_IP:8080
CADVISOR=http://$DOCKER_IP:4194

validateService()
{
  echo "Waiting for $1"
  while true; do
    curl -s -o /dev/null --connect-timeout 1 $2 && break || sleep 1
  done
}

validateService "Fabric8 console" $FABRIC8_CONSOLE
validateService "Docker registry" $DOCKER_REGISTRY
validateService "Influxdb" $INFLUXDB
validateService "Elasticsearch" $ELASTICSEARCH
validateService "Kubernetes master" $KUBERNETES
validateService "cadvisor" $CADVISOR
#validateService "Kibana console" $KIBANA_CONSOLE

# temorary workaround to update the fluentd config with the Elastic Search service ip see https://github.com/GoogleCloudPlatform/kubernetes/blob/master/contrib/logging/fluentd-es-image/td-agent.conf#L8
ELASTICSEARCH_CID=$(docker ps | grep kubernetes/fluentd-elasticsearch | cut -c 1-12)
docker exec $ELASTICSEARCH_CID bash -c 'sed -i "s/host.*$/host\ $ELASTICSEARCH_SERVICE_HOST/" /etc/td-agent/td-agent.conf'
docker exec $ELASTICSEARCH_CID bash -c 'sed -i "s/port.*$/port\ $ELASTICSEARCH_SERVICE_PORT/" /etc/td-agent/td-agent.conf'
docker restart $ELASTICSEARCH_CID > /dev/null 2>&1
# annoying hack finished

echo
echo "You're all up & running! Here are the available services:"
echo
SERVICE_TABLE="Service|URL\n-------|---"
SERVICE_TABLE="$SERVICE_TABLE\nFabric8 console|$FABRIC8_CONSOLE"
SERVICE_TABLE="$SERVICE_TABLE\nKibana console|$KIBANA_CONSOLE"
SERVICE_TABLE="$SERVICE_TABLE\nDocker Registry|$DOCKER_REGISTRY"
SERVICE_TABLE="$SERVICE_TABLE\nInfluxdb|$INFLUXDB"
SERVICE_TABLE="$SERVICE_TABLE\nElasticsearch|$ELASTICSEARCH"
SERVICE_TABLE="$SERVICE_TABLE\nKubernetes master|$KUBERNETES"
SERVICE_TABLE="$SERVICE_TABLE\nCadvisor|$CADVISOR"

printf "$SERVICE_TABLE" | column -t -s '|'

if [[ $OSTYPE == darwin* ]]; then
  open "${FABRIC8_CONSOLE}kubernetes/overview" &> /dev/null &
else
  xdg-open "${FABRIC8_CONSOLE}kubernetes/overview" &> /dev/null &
fi

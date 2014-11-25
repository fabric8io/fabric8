#!/bin/bash

#
# Discover the APP_BASE from the location of this script.
#
if [ -z "$APP_BASE" ] ; then
  DIRNAME=`dirname "$0"`
  APP_BASE=`cd "$DIRNAME"; pwd`
  export APP_BASE
fi

OPENSHIFT_IMAGE=openshift/origin:latest
OPENSHIFT_ROUTER_IMAGE=openshift/origin-haproxy-router:latest
REGISTRY_IMAGE=registry:latest
CADVISOR_IMAGE=google/cadvisor:0.6.2
INFLUXDB_IMAGE=tutum/influxdb:latest
FABRIC8_CONSOLE_IMAGE=fabric8/hawtio:latest
KIBANA_IMAGE=jimmidyson/kibana4:latest
ELASTICSEARCH_IMAGE=dockerfile/elasticsearch:latest
LOGSPOUT_IMAGE=jimmidyson/logspout-kube:latest

MINIMUM_IMAGES="${OPENSHIFT_IMAGE} ${FABRIC8_CONSOLE_IMAGE}"
ALL_IMAGES="${MINIMUM_IMAGES} ${OPENSHIFT_ROUTER_IMAGE} ${REGISTRY_IMAGE} ${CADVISOR_IMAGE} ${INFLUXDB_IMAGE} ${KIBANA_IMAGE} ${ELASTICSEARCH_IMAGE} ${LOGSPOUT_IMAGE}"
DEPLOY_IMAGES="${MINIMUM_IMAGES}"
UPDATE_IMAGES=0
DEPLOY_ALL=0

while getopts "fud:k" opt; do
  case $opt in
    k)
      DEPLOY_IMAGES="${ALL_IMAGES}"
      DEPLOY_ALL=1
      ;;
    f)
      echo "Cleaning up all existing k8s containers"
      docker rm -f openshift cadvisor || true
      RUNNING_CONTAINERS=`docker ps -a | grep k8s | cut -c 1-12`
      test -z "$RUNNING_CONTAINERS" || docker rm -f $RUNNING_CONTAINERS
      echo
      ;;
    u)
      UPDATE_IMAGES=1
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

echo "Validating your environment..."
echo

if [ ${UPDATE_IMAGES} -eq 1 ]; then
  echo "Updating all necessary images"
  for image in ${DEPLOY_IMAGES}; do
    docker pull $image
  done
  echo
fi

for image in ${DEPLOY_IMAGES}; do
  (
    IFS=':' read -a splitimage <<< "$image"
    docker images | grep -qEo "${splitimage[0]}\W+${splitimage[1]}" || (echo "Missing necessary Docker image: $image" && docker pull $image && echo)
  )
done

echo "Validating firewall rules"
RULE="INPUT -d 172.17.42.1 -s 172.17.0.0/16 -j ACCEPT"
RULE_OUTPUT=$( { docker run --rm --privileged --net=host --entrypoint=iptables ${OPENSHIFT_IMAGE} -C $RULE; } 2>&1 )
test -n "$RULE_OUTPUT" && docker run --rm --privileged --net=host --entrypoint=iptables ${OPENSHIFT_IMAGE} -I $RULE
RULE="INPUT -d 172.17.0.0/16 -s 172.121.0.0/16 -j ACCEPT"
RULE_OUTPUT=$( { docker run --rm --privileged --net=host --entrypoint=iptables ${OPENSHIFT_IMAGE} -C $RULE; } 2>&1 )
test -n "$RULE_OUTPUT" && docker run --rm --privileged --net=host --entrypoint=iptables ${OPENSHIFT_IMAGE} -I $RULE
RULE="INPUT -d 172.121.0.0/16 -s 172.17.0.0/16 -j ACCEPT"
RULE_OUTPUT=$( { docker run --rm --privileged --net=host --entrypoint=iptables ${OPENSHIFT_IMAGE} -C $RULE; } 2>&1 )
test -n "$RULE_OUTPUT" && docker run --rm --privileged --net=host --entrypoint=iptables ${OPENSHIFT_IMAGE} -I $RULE
echo

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
KUBE="docker run --rm -i --net=host ${OPENSHIFT_IMAGE} kube"

OPENSHIFT_CONTAINER=$(docker run -d --name=openshift -v /var/run/docker.sock:/var/run/docker.sock --privileged --net=host ${OPENSHIFT_IMAGE} start)

if [ ${DEPLOY_ALL} -eq 1 ]; then
  # Have to run it privileged otherwise not working on CentOS7
  CADVISOR_CONTAINER=$(docker run -d --name=cadvisor --privileged -p 4194:8080 \
    --volume=/:/rootfs:ro \
    --volume=/var/run:/var/run:rw \
    --volume=/sys:/sys:ro \
    --volume=/var/lib/docker/:/var/lib/docker:ro \
    ${CADVISOR_IMAGE})
fi

if [ -f "$APP_BASE/fabric8.json" ]; then
  cat $APP_BASE/fabric8.json | $KUBE apply -c -
  if [ ${DEPLOY_ALL} -eq 1 ]; then
    cat $APP_BASE/registry.json | $KUBE apply -c -
    cat $APP_BASE/influxdb.json | $KUBE apply -c -
    cat $APP_BASE/elasticsearch.json | $KUBE apply -c -
    cat $APP_BASE/logspout.yml | $KUBE apply -c -
    cat $APP_BASE/kibana.yml | $KUBE apply -c -
    cat $APP_BASE/router.json | $KUBE create pods -c -
  fi
else
  $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/fabric8.json
  if [ ${DEPLOY_ALL} -eq 1 ]; then
    $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/registry.json
    $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/influxdb.json
    $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/elasticsearch.json
    $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/logspout.yml
    $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/kibana.yml
    $KUBE create pods -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/router.json
  fi
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
if [ ${DEPLOY_ALL} -eq 1 ]; then
  validateService "Docker registry" $DOCKER_REGISTRY
  validateService "Influxdb" $INFLUXDB
  validateService "Elasticsearch" $ELASTICSEARCH
  validateService "Kubernetes master" $KUBERNETES
  validateService "cadvisor" $CADVISOR
  validateService "Kibana console" $KIBANA_CONSOLE

  # Set up Kibana default index
  if [ "404" == $(curl -I "${ELASTICSEARCH}/.kibana/index-pattern/\[logstash-\]YYYY.MM.DD" -w "%{http_code}" -o /dev/null -s) ]; then
    curl -s -XPUT "${ELASTICSEARCH}/.kibana/index-pattern/\[logstash-\]YYYY.MM.DD" -d '{
      "title": "[logstash-]YYYY.MM.DD",
      "timeFieldName": "@timestamp",
      "intervalName": "days",
      "customFormats": "{}",
      "fields": "[{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"stream\",\"count\":0},{\"type\":\"string\",\"indexed\":false,\"analyzed\":false,\"name\":\"_source\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"tag\",\"count\":0},{\"type\":\"string\",\"indexed\":false,\"analyzed\":false,\"name\":\"_index\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"log\",\"count\":0},{\"type\":\"date\",\"indexed\":true,\"analyzed\":false,\"doc_values\":false,\"name\":\"@timestamp\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":false,\"name\":\"_type\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":false,\"name\":\"_id\",\"count\":0}]"
    }' > /dev/null
  fi

  if [ "404" == $(curl -I "${ELASTICSEARCH}/.kibana/config/4.0.0-BETA2" -w "%{http_code}" -o /dev/null -s) ]; then
    curl -s -XPUT "${ELASTICSEARCH}/.kibana/config/4.0.0-BETA2" -d '{
      "defaultIndex": "[logstash-]YYYY.MM.DD"
    }' > /dev/null
  fi
fi

echo
echo "You're all up & running! Here are the available services:"
echo
SERVICE_TABLE="Service|URL\n-------|---"
SERVICE_TABLE="$SERVICE_TABLE\nFabric8 console|$FABRIC8_CONSOLE"
if [ ${DEPLOY_ALL} -eq 1 ]; then
  SERVICE_TABLE="$SERVICE_TABLE\nKibana console|$KIBANA_CONSOLE"
  SERVICE_TABLE="$SERVICE_TABLE\nDocker Registry|$DOCKER_REGISTRY"
  SERVICE_TABLE="$SERVICE_TABLE\nInfluxdb|$INFLUXDB"
  SERVICE_TABLE="$SERVICE_TABLE\nElasticsearch|$ELASTICSEARCH"
  SERVICE_TABLE="$SERVICE_TABLE\nKubernetes master|$KUBERNETES"
  SERVICE_TABLE="$SERVICE_TABLE\nCadvisor|$CADVISOR"
fi

printf "$SERVICE_TABLE" | column -t -s '|'

if [[ $OSTYPE == darwin* ]]; then
  open "${FABRIC8_CONSOLE}kubernetes/overview" &> /dev/null &
else
  xdg-open "${FABRIC8_CONSOLE}kubernetes/overview" &> /dev/null &
fi

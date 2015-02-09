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
CADVISOR_IMAGE=google/cadvisor:0.8.0
INFLUXDB_IMAGE=tutum/influxdb:latest
FABRIC8_CONSOLE_IMAGE=fabric8/hawtio:latest
KIBANA_IMAGE=jimmidyson/kibana4:latest
ELASTICSEARCH_IMAGE=dockerfile/elasticsearch:latest
LOGSPOUT_IMAGE=jimmidyson/logspout-kube:latest
GRAFANA_IMAGE=jimmidyson/grafana:latest

MINIMUM_IMAGES="${OPENSHIFT_IMAGE} ${FABRIC8_CONSOLE_IMAGE} ${REGISTRY_IMAGE}"
ALL_IMAGES="${MINIMUM_IMAGES} ${OPENSHIFT_ROUTER_IMAGE} ${CADVISOR_IMAGE} ${INFLUXDB_IMAGE} ${KIBANA_IMAGE} ${ELASTICSEARCH_IMAGE} ${LOGSPOUT_IMAGE} ${GRAFANA_IMAGE}"
DEPLOY_IMAGES="${MINIMUM_IMAGES}"
UPDATE_IMAGES=0
DEPLOY_ALL=0
CLEANUP=0
DONT_RUN=0

while getopts "fud:kp" opt; do
  case $opt in
    k)
      DEPLOY_IMAGES="${ALL_IMAGES}"
      DEPLOY_ALL=1
      ;;
    f)
      echo "Cleaning up all existing k8s containers"
      docker rm -fv openshift cadvisor || true
      RUNNING_CONTAINERS=`docker ps -a | grep k8s | cut -c 1-12`
      test -z "$RUNNING_CONTAINERS" || docker rm -fv $RUNNING_CONTAINERS
      CLEANUP=1
      echo
      ;;
    u)
      UPDATE_IMAGES=1
      ;;
    p)
      UPDATE_IMAGES=1
      DONT_RUN=1
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

if [ ${CLEANUP} -eq 1 ]; then
  docker run --rm --privileged -v /var/lib:/var/lib --entrypoint=rm ${OPENSHIFT_IMAGE} -rf /var/lib/openshift/
fi

if [ ${DONT_RUN} -eq 1 ]; then
  echo "Terminating before running any containers"
  exit 0
fi

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
RULE="INPUT -d 172.30.17.0/24 -s 172.17.0.0/16 -j ACCEPT"
RULE_OUTPUT=$( { docker run --rm --privileged --net=host --entrypoint=iptables ${OPENSHIFT_IMAGE} -C $RULE; } 2>&1 )
test -n "$RULE_OUTPUT" && docker run --rm --privileged --net=host --entrypoint=iptables ${OPENSHIFT_IMAGE} -I $RULE
RULE="INPUT -d 172.17.0.0/16 -s 172.30.17.0/24 -j ACCEPT"
RULE_OUTPUT=$( { docker run --rm --privileged --net=host --entrypoint=iptables ${OPENSHIFT_IMAGE} -C $RULE; } 2>&1 )
test -n "$RULE_OUTPUT" && docker run --rm --privileged --net=host --entrypoint=iptables ${OPENSHIFT_IMAGE} -I $RULE
echo

# TODO it would be nice if we could tell easily if these routes have already been applied so we don't have to do this each time
if [[ $OSTYPE == darwin* ]]; then
    if [ -z "$DOCKER_IP" ] ; then
      export DOCKER_IP=`boot2docker ip 2> /dev/null`
    fi

    echo "Adding network routes to 172.17.0.0/24, 172.30.17.0/24 & 172.121.17.0/24 via $DOCKER_IP so that the host operating system can see pods and services inside OpenShift"
    sudo route -n add 172.17.0.0/24 $DOCKER_IP
    sudo route -n add 172.30.17.0/24 $DOCKER_IP
    sudo route -n add 172.121.17.0/24 $DOCKER_IP
fi

export DOCKER_IP=${DOCKER_IP:-127.0.0.1}
export KUBERNETES=http://$DOCKER_IP:8443

# using an env var but ideally we'd use an alias ;)
KUBE="docker run --rm -i --net=host ${OPENSHIFT_IMAGE} cli --insecure-skip-tls-verify=true"

OPENSHIFT_CONTAINER=$(docker run -d --name=openshift -v /var/run/docker.sock:/var/run/docker.sock -v /var/lib/openshift:/var/lib/openshift --privileged --net=host ${OPENSHIFT_IMAGE} start --portal-net='172.30.17.0/24' --cors-allowed-origins='.*')

validateService()
{
  echo "Waiting for $1"
  while true; do
    curl -k -s -o /dev/null --connect-timeout 1 $2 && break || sleep 1
  done
}

validateService "Kubernetes master" $KUBERNETES

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
  cat $APP_BASE/fabric8.json | $KUBE apply -f -
  cat $APP_BASE/registry.json | $KUBE apply -f -
  if [ ${DEPLOY_ALL} -eq 1 ]; then
    cat $APP_BASE/influxdb.json | $KUBE apply -f -
    cat $APP_BASE/elasticsearch.json | $KUBE apply -f -
    cat $APP_BASE/logspout.yml | $KUBE apply -f -
    cat $APP_BASE/kibana.yml | $KUBE apply -f -
    cat $APP_BASE/grafana.yml | $KUBE apply -f -
    cat $APP_BASE/router.json | $KUBE create  -f -
  fi
else
  curl -s https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/fabric8.json | $KUBE apply -f -
  curl -s https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/registry.json | $KUBE apply -f -
  if [ ${DEPLOY_ALL} -eq 1 ]; then
    curl -s https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/influxdb.json | $KUBE apply -f -
    curl -s https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/elasticsearch.json | $KUBE apply -f -
    curl -s https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/logspout.yml | $KUBE apply -f -
    curl -s https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/kibana.yml | $KUBE apply -f -
    curl -s https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/grafana.yml | $KUBE apply -f -
    curl -s https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/router.json | $KUBE create -f -
  fi
fi

K8S_SERVICES=$($KUBE get services)

echo
echo "Waiting for services to fully come up - shouldn't be too long for you to wait"
echo

getServiceIpAndPort()
{
  echo `echo "$1"|grep $2| sed 's/\s\+/ /g' | awk '{ print $4 ":" $5 }'`
}

FABRIC8_CONSOLE=http://$(getServiceIpAndPort "$K8S_SERVICES" fabric8-console)/hawtio/
DOCKER_REGISTRY=http://$(getServiceIpAndPort "$K8S_SERVICES" registry)
INFLUXDB=http://$(getServiceIpAndPort "$K8S_SERVICES" influxdb-service)
ELASTICSEARCH=http://$(getServiceIpAndPort "$K8S_SERVICES" elasticsearch)
KIBANA_CONSOLE=http://$(getServiceIpAndPort "$K8S_SERVICES" kibana-service)
GRAFANA_CONSOLE=http://$(getServiceIpAndPort "$K8S_SERVICES" grafana-service)
CADVISOR=http://$DOCKER_IP:4194

validateService "Fabric8 console" $FABRIC8_CONSOLE
if [ ${DEPLOY_ALL} -eq 1 ]; then
  validateService "Docker registry" $DOCKER_REGISTRY
  validateService "Influxdb" $INFLUXDB
  validateService "Elasticsearch" $ELASTICSEARCH
  validateService "cadvisor" $CADVISOR
  validateService "Kibana console" $KIBANA_CONSOLE
  validateService "Grafana console" $GRAFANA_CONSOLE

  # Set up Kibana default index
  if [ "404" == $(curl -s -I "${ELASTICSEARCH}/.kibana/index-pattern/\[logstash-\]YYYY.MM.DD" -w "%{http_code}" -o /dev/null) ]; then
    curl -s -XPUT "${ELASTICSEARCH}/.kibana/index-pattern/\[logstash-\]YYYY.MM.DD" -d '{
      "title": "[logstash-]YYYY.MM.DD",
      "timeFieldName": "@timestamp",
      "intervalName": "days",
      "customFormats": "{}",
      "fields": "[{\"type\":\"string\",\"indexed\":false,\"analyzed\":false,\"name\":\"_source\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"query_string\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":false,\"name\":\"_type\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"level\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":false,\"name\":\"_id\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"name\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"path\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"k8s_pod\",\"count\":2},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"status\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"k8s_namespace\",\"count\":2},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"remote_addr\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"http_version\",\"count\":0},{\"type\":\"string\",\"indexed\":false,\"analyzed\":false,\"name\":\"_index\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"image\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"message\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request_method\",\"count\":0},{\"type\":\"date\",\"indexed\":true,\"analyzed\":false,\"doc_values\":false,\"name\":\"@timestamp\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"k8s_container\",\"count\":10},{\"type\":\"number\",\"indexed\":true,\"analyzed\":false,\"doc_values\":false,\"name\":\"response_time\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"container\",\"count\":0},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"content_length\",\"count\":0}]"
    }' > /dev/null
  fi

  if [ "404" == $(curl -s -I "${ELASTICSEARCH}/.kibana/config/4.0.0-BETA2" -w "%{http_code}" -o /dev/null) ]; then
    curl -s -XPUT "${ELASTICSEARCH}/.kibana/config/4.0.0-BETA2" -d '{
      "defaultIndex": "[logstash-]YYYY.MM.DD"
    }' > /dev/null
  fi

  if [ "404" == $(curl -s -I "${ELASTICSEARCH}/.kibana/search/Fabric8" -w "%{http_code}" -o /dev/null) ]; then
    curl -s -XPUT "${ELASTICSEARCH}/.kibana/search/Fabric8" -d '{
      "title": "Fabric8",
      "description": "",
      "hits": 0,
      "columns": [
        "container",
        "image",
        "message",
        "k8s_namespace",
        "k8s_pod",
        "k8s_container"
      ],
      "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{\"query\":{\"query_string\":{\"query\":\"*\"}},\"filter\":[],\"index\":\"[logstash-]YYYY.MM.DD\"}"
      }
    }' > /dev/null
  fi
fi

echo
echo "You're all up & running! Here are the available services:"
echo
header="%-20s | %-60s\n"
format="%-20s | %-60s\n"
printf "${header}" Service URL
printf "${header}" "-------" "---"
printf "${format}" "Fabric8 console" $FABRIC8_CONSOLE
printf "${format}" "Docker Registry" $DOCKER_REGISTRY
if [ ${DEPLOY_ALL} -eq 1 ]; then
  printf "${format}" "Kibana console" $KIBANA_CONSOLE
  printf "${format}" "Grafana console" $GRAFANA_CONSOLE
  printf "${format}" "Influxdb" $INFLUXDB
  printf "${format}" "Elasticsearch" $ELASTICSEARCH
  printf "${format}" "Kubernetes master" $KUBERNETES
  printf "${format}" "Cadvisor" $CADVISOR
fi

printf "$SERVICE_TABLE" | column -t -s '|'

if [[ $OSTYPE == darwin* ]]; then
  open "${FABRIC8_CONSOLE}kubernetes/overview" &> /dev/null &
else
  xdg-open "${FABRIC8_CONSOLE}kubernetes/overview" &> /dev/null &
fi

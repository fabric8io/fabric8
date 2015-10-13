#!/bin/bash

# Preconditions
REQUIRED_DOCKER_VERSION=1.6
DOCKER_VERSION=`docker version | grep 'Server version' | cut -d ' ' -f 3`
if [[ "$DOCKER_VERSION" < "$REQUIRED_DOCKER_VERSION" ]]; then
  echo "Docker ${REQUIRED_DOCKER_VERSION} is required to run Fabric8."
  exit -1
fi

#
# Discover the APP_BASE from the location of this script.
#
if [ -z "$APP_BASE" ] ; then
  DIRNAME=`dirname "$0"`
  APP_BASE=`cd "$DIRNAME"; pwd`
  export APP_BASE
fi

OPENSHIFT_VERSION=v0.5.3

FABRIC8_VERSION=2.0.47
OPENSHIFT_IMAGE=openshift/origin:${OPENSHIFT_VERSION}
OPENSHIFT_ROUTER_IMAGE=openshift/origin-haproxy-router:${OPENSHIFT_VERSION}
REGISTRY_IMAGE=openshift/origin-docker-registry:${OPENSHIFT_VERSION}
INFLUXDB_IMAGE=tutum/influxdb:latest
FABRIC8_CONSOLE_IMAGE=fabric8/hawtio-kubernetes:latest
KIBANA_IMAGE=jimmidyson/kibana4:latest
ELASTICSEARCH_IMAGE=fabric8/elasticsearch-k8s:1.5.0
FLUENTD_IMAGE=fabric8/fluentd-kubernetes:latest
#GRAFANA_IMAGE=jimmidyson/grafana:latest
APP_LIBRARY_IMAGE=fabric8/app-library:${FABRIC8_VERSION}
FORGE_IMAGE=fabric8/fabric8-forge:${FABRIC8_VERSION}
KIWIIRC_IMAGE=fabric8/fabric8-kiwiirc:latest
REDIS_IMAGE=redis:latest
GOIRC_SERVER_IMAGE=lonli078/go-irc-server:latest
HUBOT_IMAGE=fabric8/hubot:latest
HUBOT_NOTIFIER=fabric8/hubot-notifier:${FABRIC8_VERSION}

MINIMUM_IMAGES="${OPENSHIFT_IMAGE} ${FABRIC8_CONSOLE_IMAGE} ${APP_LIBRARY_IMAGE} ${REGISTRY_IMAGE} ${OPENSHIFT_ROUTER_IMAGE}"
ALL_IMAGES="${MINIMUM_IMAGES} ${INFLUXDB_IMAGE} ${KIBANA_IMAGE} ${ELASTICSEARCH_IMAGE} ${FLUENTD_IMAGE} ${GRAFANA_IMAGE} ${FORGE_IMAGE} ${KIWIIRC_IMAGE} ${REDIS_IMAGE} ${GOIRC_SERVER_IMAGE} ${HUBOT_IMAGE} ${HUBOT_NOTIFIER}"
DEPLOY_IMAGES="${MINIMUM_IMAGES}"
UPDATE_IMAGES=0
DEPLOY_ALL=0
CLEANUP=0
DONT_RUN=0
FABRIC8_VAGRANT_IP=172.28.128.4
OPENSHIFT_ADMIN_PASSWORD=admin
OPENSHIFT_MASTER_URL=localhost

while getopts "fud:kpm:P:" opt; do
  case $opt in
    k)
      DEPLOY_IMAGES="${ALL_IMAGES}"
      DEPLOY_ALL=1
      ;;
    f)
      echo "Cleaning up all existing k8s containers"
      docker rm -fv openshift || true
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
    m)
      OPENSHIFT_MASTER_URL=${OPTARG##http*://}
      ;;
    P)
      OPENSHIFT_ADMIN_PASSWORD=$OPTARG
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
  docker run --rm --privileged -v /var/lib:/var/lib -v /var/log:/var/log --entrypoint=rm ${OPENSHIFT_IMAGE} -rf /var/lib/openshift/ /var/log/containers
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
    sudo route delete 172.17.0.0
    sudo route -n add 172.17.0.0/24 $DOCKER_IP
    sudo route delete 172.30.17.0
    sudo route -n add 172.30.17.0/24 $DOCKER_IP
    sudo route delete 172.121.17.0
    sudo route -n add 172.121.17.0/24 $DOCKER_IP
fi

export DOCKER_IP=${DOCKER_IP:-127.0.0.1}
export KUBERNETES=https://$DOCKER_IP:8443

# using an env var but ideally we'd use an alias ;)
KUBE="docker exec openshift osc"

if [ -n "${OPENSHIFT_MASTER_URL}" ]; then
  PUBLIC_MASTER_ARG="--public-master=${OPENSHIFT_MASTER_URL}"
fi

#if [ -n "${OPENSHIFT_ADMIN_PASSWORD}" ]; then
#  echo "Configuring OpenShift authentication"
#  docker run -v /openshift gliderlabs/alpine:3.1 sh -c "apk-install apache2-utils && htpasswd -bc /openshift/htpasswd admin ${OPENSHIFT_ADMIN_PASSWORD}" &> /dev/null
#  OPENSHIFT_VOLUME_MOUNT="--volumes-from=$(docker ps -ql)"
#  OPENSHIFT_OAUTH_ARGS="-e OPENSHIFT_OAUTH_PASSWORD_AUTH=htpasswd -e OPENSHIFT_OAUTH_HTPASSWD_FILE=/openshift/htpasswd"
#fi

OPENSHIFT_CONTAINER=$(docker run -d --name=openshift ${OPENSHIFT_VOLUME_MOUNT} -v /var/run/docker.sock:/var/run/docker.sock -v /var/lib/openshift:/var/lib/openshift -v /var/log/containers:/var/log/containers --privileged --net=host ${OPENSHIFT_IMAGE} start --portal-net='172.30.17.0/24' --cors-allowed-origins='.*' ${PUBLIC_MASTER_ARG})

validateService()
{
  echo "Waiting for $1"
  while true; do
    curl -k -s -o /dev/null --connect-timeout 1 $2 && break || sleep 1
  done
}

validateService "Kubernetes master" $KUBERNETES

while true; do
  (docker exec openshift osc get namespaces default | grep default) && break || sleep 1
done

sleep 30

docker exec openshift sh -c "osadm router --credentials=openshift.local.config/master/openshift-router.kubeconfig --create"
docker exec openshift sh -c "osadm registry --credentials=openshift.local.config/master/openshift-registry.kubeconfig --create"
docker exec openshift sh -c "osadm policy add-cluster-role-to-user cluster-admin admin"

cat <<EOF | docker exec -i openshift osc create -f -
---
  apiVersion: "v1beta3"
  kind: "Secret"
  metadata:
    name: "openshift-cert-secrets"
  data:
    root-cert: "$(docker exec openshift base64 -w 0 /var/lib/openshift/openshift.local.config/master/ca.crt)"
    admin-cert: "$(docker exec openshift base64 -w 0 /var/lib/openshift/openshift.local.config/master/admin.crt)"
    admin-key: "$(docker exec openshift base64 -w 0 /var/lib/openshift/openshift.local.config/master/admin.key)"
EOF

deployFabric8Console() {
  cat <<EOF | docker exec -i openshift osc create -f -
---
  id: "fabric8-config"
  kind: "Config"
  apiVersion: "v1beta1"
  name: "fabric8-config"
  description: "Creates a hawtio console"
  items:
    - apiVersion: "v1beta1"
      containerPort: 9090
      id: "fabric8-console-service"
      kind: "Service"
      port: 80
      selector:
        component: "fabric8Console"
    - apiVersion: "v1beta1"
      desiredState:
        podTemplate:
          desiredState:
            manifest:
              containers:
                - image: "fabric8/hawtio-kubernetes:latest"
                  name: "fabric8-console-container"
                  imagePullPolicy: "PullIfNotPresent"
                  env:
                    - name: OAUTH_CLIENT_ID
                      value: fabric8
                    - name: OAUTH_AUTHORIZE_URI
                      value: https://${OPENSHIFT_MASTER_URL}:8443/oauth/authorize
                  ports:
                    - containerPort: 9090
                      protocol: "TCP"
                  volumeMounts:
                    - name: openshift-cert-secrets
                      mountPath: /etc/secret-volume
                      readOnly: true
              id: "hawtioPod"
              version: "v1beta1"
              volumes:
                - name: openshift-cert-secrets
                  source:
                    secret:
                      target:
                        kind: Secret
                        namespace: default
                        name: openshift-cert-secrets
          labels:
            component: "fabric8Console"
        replicaSelector:
          component: "fabric8Console"
        replicas: 1
      id: "fabric8-console-controller"
      kind: "ReplicationController"
      labels:
        component: "fabric8ConsoleController"
EOF
}

for app in app-library fabric8-forge; do
  $KUBE create -f http://central.maven.org/maven2/io/fabric8/jube/images/fabric8/${app}/${FABRIC8_VERSION}/${app}-${FABRIC8_VERSION}-kubernetes.json
done

deployFabric8Console

if [ ${DEPLOY_ALL} -eq 1 ]; then
  for app in gogs hubot hubot-notifier lets-chat cdelivery influxdb elasticsearch kibana orion taiga; do
    $KUBE create -f  http://central.maven.org/maven2/io/fabric8/jube/images/fabric8/${app}/${FABRIC8_VERSION}/${app}-${FABRIC8_VERSION}-kubernetes.json
  done
  $KUBE create -f https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/fluentd.yml
fi

K8S_SERVICES=$($KUBE get services)

echo
echo "Waiting for services to fully come up - shouldn't be too long for you to wait"
echo

getServiceIpAndPort()
{
  echo `echo "$1"|grep "$2"| sed -e 's/\s\+/ /g' -e 's/\/[tT][cC][pP]//gI' -e 's/\/[uU][dD][pP]//gI' | awk '{ print $4 ":" $5 }'`
}

getServiceIp()
{
  echo `echo "$1"|grep $2| sed 's/\s\+/ /g' | awk '{ print $4 }'`
}

FABRIC8_CONSOLE=$(getServiceIp "$K8S_SERVICES" fabric8-console)/
DOCKER_REGISTRY=$(getServiceIpAndPort "$K8S_SERVICES" docker-registry)
INFLUXDB=http://$(getServiceIpAndPort "$K8S_SERVICES" influxdb-service)
ELASTICSEARCH=http://$(getServiceIpAndPort "$K8S_SERVICES" 'elasticsearch ')
KIBANA_CONSOLE=http://$(getServiceIpAndPort "$K8S_SERVICES" kibana-service)
#GRAFANA_CONSOLE=http://$(getServiceIpAndPort "$K8S_SERVICES" grafana-service)

validateService "Fabric8 console" $FABRIC8_CONSOLE

if [ -n "${OPENSHIFT_MASTER_URL}" ]; then
  FABRIC8_CONSOLE=${OPENSHIFT_MASTER_URL}

  echo "Configuring OpenShift routes for Fabric8"

  cat <<EOF | docker exec -i openshift osc create -f -
{
  "id": "routes-list",
  "kind": "List",
  "apiVersion": "v1beta2",
  "name": "routes-config",
  "items": [
    {
      "id": "docker-registry-route",
      "metadata": {
        "name": "docker-registry-route"
      },
      "apiVersion": "v1beta1",
      "kind": "Route",
      "host": "registry.${FABRIC8_CONSOLE}",
      "serviceName": "docker-registry"
    },
    {
      "id": "fabric8-console-route",
      "metadata": {
        "name": "fabric8-console-route"
      },
      "apiVersion": "v1beta1",
      "kind": "Route",
      "host": "${FABRIC8_CONSOLE}",
      "serviceName": "fabric8-console-service"
    },
    {
      "id": "fabric8-logs-route",
      "metadata": {
        "name": "fabric8-logs-route"
      },
      "apiVersion": "v1beta1",
      "kind": "Route",
      "host": "logs.${FABRIC8_CONSOLE}",
      "serviceName": "kibana-service"
    },
    {
      "id": "fabric8-metrics-route",
      "metadata": {
        "name": "fabric8-metrics-console-route"
      },
      "apiVersion": "v1beta1",
      "kind": "Route",
      "host": "metrics.${FABRIC8_CONSOLE}",
      "serviceName": "grafana-service"
    },
    {
      "id": "letschat-route",
      "metadata": {
        "name": "letschat-route"
      },
      "apiVersion": "v1beta1",
      "kind": "Route",
      "host": "letschat.${FABRIC8_CONSOLE}",
      "serviceName": "letschat"
    },
    {
      "id": "gogs-http-service-route",
      "metadata": {
        "name": "gogs-http-service-route"
      },
      "apiVersion": "v1beta1",
      "kind": "Route",
      "host": "gogs.${FABRIC8_CONSOLE}",
      "serviceName": "gogs-http-service"
    },
    {
      "id": "orion-route",
      "metadata": {
        "name": "orion-route"
      },
      "apiVersion": "v1beta1",
      "kind": "Route",
      "host": "orion.${FABRIC8_CONSOLE}",
      "serviceName": "orion"
    },
    {
      "id": "taiga-route",
      "metadata": {
        "name": "taiga-route"
      },
      "apiVersion": "v1beta1",
      "kind": "Route",
      "host": "taiga.${FABRIC8_CONSOLE}",
      "serviceName": "taiga"
    }
  ]
}
EOF
fi

# TODO enable on gogs route when we can use https
#
#      "tls": {
#        "termination": "passthrough"
#       }


echo "Configuring OpenShift oauth"

cat <<EOF | docker exec -i openshift osc create -f -
{
  "kind": "OAuthClient",
  "apiVersion": "v1beta1",
  "metadata": {
    "name": "fabric8"
  },
  "redirectURIs": [
    "http://localhost:9090",
    "http://localhost:2772",
    "http://localhost:9000",
    "http://localhost:3000",
    "http://${FABRIC8_CONSOLE}",
    "https://${FABRIC8_CONSOLE}"
  ]
}
EOF

echo

validateService "Docker registry" $DOCKER_REGISTRY
if [ ${DEPLOY_ALL} -eq 1 ]; then
  validateService "Influxdb" $INFLUXDB
  validateService "Elasticsearch" $ELASTICSEARCH
  validateService "Kibana console" $KIBANA_CONSOLE
#  validateService "Grafana console" $GRAFANA_CONSOLE

  # Set up Kibana default index
  if [ "404" == $(curl -s -I "${ELASTICSEARCH}/.kibana/index-pattern/\[logstash-\]YYYY.MM.DD" -w "%{http_code}" -o /dev/null) ]; then
    curl -s -XPUT "${ELASTICSEARCH}/.kibana/index-pattern/\[logstash-\]YYYY.MM.DD" -d '{
      "title": "[logstash-]YYYY.MM.DD",
      "timeFieldName": "@timestamp",
      "intervalName": "days",
      "customFormats": "{}",
      "fields": "[{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request.headers.if-none-match\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":false,\"analyzed\":false,\"name\":\"_index\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"pod\",\"count\":1,\"scripted\":false},{\"type\":\"number\",\"indexed\":true,\"analyzed\":false,\"doc_values\":false,\"name\":\"response.statusCode\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"log\",\"count\":6,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"node_env\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request.headers.accept\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request.headers.accept-language\",\"count\":0,\"scripted\":false},{\"type\":\"number\",\"indexed\":true,\"analyzed\":false,\"doc_values\":false,\"name\":\"response.contentLength\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"error\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request.headers.host\",\"count\":0,\"scripted\":false},{\"type\":\"number\",\"indexed\":true,\"analyzed\":false,\"doc_values\":false,\"name\":\"request.remotePort\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request.headers.content-type\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"stream\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request.remoteAddress\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request.headers.content-length\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request.headers.accept-encoding\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"level\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request.headers.referer\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":false,\"name\":\"_type\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request.method\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request.url\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"message\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request.headers.origin\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request.headers.user-agent\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"pod_namespace\",\"count\":1,\"scripted\":false},{\"type\":\"date\",\"indexed\":true,\"analyzed\":false,\"doc_values\":false,\"name\":\"@timestamp\",\"count\":1,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"container_name\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"pod_container\",\"count\":1,\"scripted\":false},{\"type\":\"number\",\"indexed\":true,\"analyzed\":false,\"doc_values\":false,\"name\":\"response.responseTime\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":false,\"analyzed\":false,\"name\":\"_source\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":false,\"analyzed\":false,\"name\":\"_id\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"container_id\",\"count\":0,\"scripted\":false},{\"type\":\"string\",\"indexed\":true,\"analyzed\":true,\"doc_values\":false,\"name\":\"request.headers.if-modified-since\",\"count\":0,\"scripted\":false}]"
    }' > /dev/null
  fi

  if [ "404" == $(curl -s -I "${ELASTICSEARCH}/.kibana/config/4.0.1" -w "%{http_code}" -o /dev/null) ]; then
    curl -s -XPUT "${ELASTICSEARCH}/.kibana/config/4.0.1" -d '{
      "defaultIndex": "[logstash-]YYYY.MM.DD"
    }' > /dev/null
  fi

  if [ "404" == $(curl -s -I "${ELASTICSEARCH}/.kibana/search/Fabric8" -w "%{http_code}" -o /dev/null) ]; then
    curl -s -XPUT "${ELASTICSEARCH}/.kibana/search/Fabric8" -d '{
      "title": "Fabric8",
      "description": "",
      "hits": 0,
      "columns": [
        "log",
        "pod",
        "pod_container",
        "pod_namespace"
      ],
      "sort": [
        "@timestamp",
        "desc"
      ],
      "version": 1,
        "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{\"query\":{\"query_string\":{\"query\":\"*\",\"analyze_wildcard\":true}},\"filter\":[],\"index\":\"[logstash-]YYYY.MM.DD\",\"highlight\":{\"pre_tags\":[\"@kibana-highlighted-field@\"],\"post_tags\":[\"@/kibana-highlighted-field@\"],\"fields\":{\"*\":{}}}}"
      }
    }' > /dev/null
  fi

  if [ "404" == $(curl -s -I "${ELASTICSEARCH}/.kibana/dashboard/Fabric8" -w "%{http_code}" -o /dev/null) ]; then
    curl -s -XPUT "${ELASTICSEARCH}/.kibana/dashboard/Fabric8" -d '{
      "title": "Fabric8",
      "hits": 0,
      "description": "",
      "panelsJSON": "[{\"col\":1,\"id\":\"Fabric8\",\"row\":1,\"size_x\":12,\"size_y\":5,\"type\":\"search\"}]",
      "version": 1,
      "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{\"filter\":[{\"query\":{\"query_string\":{\"analyze_wildcard\":true,\"query\":\"*\"}}}]}"
      }
    }' > /dev/null
  fi
fi

# Work out the host machine related env vars.  There can be multiple networy interfaces so if we dont know which one, the options are listed for the user to decide.
function printHostEnvVars {
  IPS=$(hostname -I)
  if echo "$IPS" | grep -q "$FABRIC8_VAGRANT_IP"; then
    printf "%s\n" "export DOCKER_IP=$FABRIC8_VAGRANT_IP"
    printf "%s\n" "export DOCKER_HOST=tcp://$FABRIC8_VAGRANT_IP:2375"
    printf "%s\n" "export KUBERNETES_MASTER=https://$FABRIC8_VAGRANT_IP:8443"
  else
    if [[ $IPS  = *[[:space:]]* ]]; then
      printf "\n"
      printf "%s\n" "# ATTENTION!!!!  Multiple potential DOCKER_IP's found, please use only ONE from the list below.  Try to ping each one to work out the correct environment variables to set."
      for IP in $IPS; do
        printf "%s\n" "#---"
        printf "%s\n" "export DOCKER_IP=$IP"
        printf "%s\n" "export DOCKER_HOST=tcp://$IP:2375"
        printf "%s\n" "export KUBERNETES_MASTER=https://$IP:8443"
        printf "%s\n" "#---"
      done
     else
       printf "%s\n" "export DOCKER_IP=$IPS"
       printf "%s\n" "export DOCKER_HOST=tcp://$IPS:2375"
       printf "%s\n" "export KUBERNETES_MASTER=https://$IPS:8443"
    fi
  fi
}

echo
echo "You're all up & running! Here are the available services:"
echo
header="%-20s | %-60s\n"
format="%-20s | %-60s\n"
printf "${header}" Service URL
printf "${header}" "-------" "---"
printf "${format}" "Kubernetes master" $KUBERNETES
printf "${format}" "Fabric8 console" http://$FABRIC8_CONSOLE
printf "${format}" "Docker Registry" $DOCKER_REGISTRY
if [ ${DEPLOY_ALL} -eq 1 ]; then
  printf "${format}" "Kibana console" $KIBANA_CONSOLE
#  printf "${format}" "Grafana console" $GRAFANA_CONSOLE
  printf "${format}" "Influxdb" $INFLUXDB
  printf "${format}" "Elasticsearch" $ELASTICSEARCH
fi

printf "$SERVICE_TABLE" | column -t -s '|'

printf "\n"
printf "%s\n" "Set these environment variables on your development machine:"
printf "\n"
printf "%s\n" "export FABRIC8_CONSOLE=http://$FABRIC8_CONSOLE"
printf "%s\n" "export DOCKER_REGISTRY=$DOCKER_REGISTRY"
if [[ -z "${DOCKER_HOST}" ]]; then
  printHostEnvVars
fi

if [[ $OSTYPE == darwin* ]]; then
  open "http://${FABRIC8_CONSOLE}/kubernetes/overview" &> /dev/null &
else
  xdg-open "http://${FABRIC8_CONSOLE}/kubernetes/overview" &> /dev/null &
fi

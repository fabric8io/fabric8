#!/bin/bash -e

#
# Discover the APP_BASE from the location of this script.
#
if [ -z "$APP_BASE" ] ; then
  DIRNAME=`dirname "$0"`
  APP_BASE=`cd "$DIRNAME"; pwd`
  export APP_BASE
fi

while getopts "fud:" opt; do
  case $opt in
    f)
      echo "Cleaning up all existing k8s containers"
      docker rm -f openshift cadvisor > /dev/null 2>&1 || true
      RUNNING_CONTAINERS=`docker ps -a | grep k8s | cut -c 1-12`
      test -z "$RUNNING_CONTAINERS" || docker rm -f $RUNNING_CONTAINERS
      ;;
    u)
      echo "Updating all necessary images"
      for image in google/cadvisor:latest openshift/origin:latest registry:latest tutum/influxdb:latest fabric8/hawtio:latest; do
        docker pull $image
      done
      ;;
    d)
      DOCKER_IP=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      ;;
  esac
done

export DOCKER_IP=${DOCKER_IP:-127.0.0.1}
export DOCKER_REGISTRY=$DOCKER_IP:5000
export KUBERNETES_MASTER=http://$DOCKER_IP:8080
export FABRIC8_CONSOLE=http://$DOCKER_IP:8484/hawtio

docker run -d --name=openshift -v /var/run/docker.sock:/var/run/docker.sock --privileged --net=host openshift/origin:latest start

docker run -d --name=cadvisor -p 4194:8080 \
  --volume=/:/rootfs:ro \
  --volume=/var/run:/var/run:rw \
  --volume=/sys:/sys:ro \
  --volume=/var/lib/docker/:/var/lib/docker:ro \
  google/cadvisor:latest

# using an env var but ideally we'd use an alias ;)
KUBE="docker run --rm -i --net=host openshift/origin:latest kube"

if [ -f "$APP_BASE/registry.json" ]; then
  cat $APP_BASE/registry.json | $KUBE apply -c -
  cat $APP_BASE/influxdb.json | $KUBE apply -c -
  cat $APP_BASE/fabric8.json | $KUBE apply -c -
  cat $APP_BASE/elasticsearch.json | $KUBE apply -c -
else
  $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/registry.json
  $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/influxdb.json
  $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/fabric8.json
  $KUBE apply -c https://raw.githubusercontent.com/fabric8io/fabric8/master/bin/elasticsearch.json
fi

getServiceIpAndPort()
{
  echo `echo "$1"|grep $2| sed 's/\s\+/ /g' | awk '{ print $3 ":" $4 }'`
}

K8S_SERVICES=`$KUBE list services`

echo
echo "You now have the following services running:"
echo
echo "Fabric8 console: http://$(getServiceIpAndPort "$K8S_SERVICES" hawtio-service)/hawtio"
echo "Docker Registry: http://$(getServiceIpAndPort "$K8S_SERVICES" registry-service)"
echo "Influxdb: http://$(getServiceIpAndPort "$K8S_SERVICES" influx-master)"
echo "Elasticsearch: http://$(getServiceIpAndPort "$K8S_SERVICES" elasticsearch)"
echo "Kubernetes master: http://$DOCKER_IP:8080"
echo "Cadvisor: http://$DOCKER_IP:4194"

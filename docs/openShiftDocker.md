## Run OpenShift V3 using Docker

Probably the easiest way to use OpenShift V3 on your laptop is to use [Docker](https://docs.docker.com/) since thats the only thing you need to install and often developers already have it.

First you'll need to [install docker](https://docs.docker.com/installation/), the later the version generally the better it is!

If you fancy starting OpenShift V3 the super-easy way, run this one-liner (after setting some [environment variables](#environment-variables) if you’re running on OS X):

    bash <(curl -sSL https://bit.ly/get-fabric8)

This will start up OpenShift in a Docker container, as well as the Fabric8 console (hawtio) & some supporting containers that are used for logging & metrics.

### Environment variables

You'll need the following environment variables to be able use the [Tools](http://fabric8.io/v2/tools.html) such as the [Console](console.html), [Maven Plugin](http://fabric8.io/v2/mavenPlugin.html), the [Forge Addons](http://fabric8.io/v2/forge.html) and the [java libraries](javaLibraries.html):

    export DOCKER_IP=`boot2docker ip 2> /dev/null`
    export DOCKER_REGISTRY=$DOCKER_IP:5000
    export KUBERNETES_MASTER=http://$DOCKER_IP:8080
    export FABRIC8_CONSOLE=http://$DOCKER_IP:8484/hawtio

Usually your $DOCKER_IP is something like **192.168.59.103** if you are on Windows or a Mac and are using boot2docker.

### Using the kube command line

Run this command or add it to your ~/.bashrc

    alias kube="docker run --rm --net=host -i -e KUBERNETES_MASTER=http://$DOCKER_IP:8080 openshift/origin:latest kube"

You can now use the kube command line to list pods, replication controllers and services; delete or create resources etc:

    kube list pods
    kube list replicationControllers
    kube list services

To see all the available commands:

    kube --help

**Note** that since we are using docker and you are typically running the docker commands from the host machine (your laptop), the kube command which runs in a linux container (which on Windows or a Mac is inside the boot2docker VM) it won't be able to access local files by file name when supplying -c to apply.

However you can pipe them into the command line via

    cat mything.json | kube apply -c -

### Network routes

To be able to connect to the pod or service IPs inside OpenShift (so that hawtio can connect into your JVMs) you'll need to run this once on your machine:

    sudo route -n add 172.17.0.0/24 $DOCKER_IP
    sudo route -n add 172.121.17.0/24 $DOCKER_IP

### Running OpenShift

    docker pull openshift/origin
    docker run -v /var/run/docker.sock:/var/run/docker.sock --net=host --privileged openshift/origin start

You should now be able to access the REST API for OpenShift on the **DOCKER_IP** address at [http://192.168.59.103:8080/api/v1beta1/pods](http://192.168.59.103:8080/api/v1beta1/pods)

### Running a local docker registry

    docker run -p 5000:5000 registry

There's a handy script called  [ping-registry.sh](https://github.com/fabric8io/fabric8/blob/master/bin/ping-registry.sh) which will check that you have your **DOCKER_REGISTRY** environment variable setup correctly to point to a valid docker registry so that you can create and push docker images:

    ping-registry.sh

### Running a hawtio console

    docker run -p 8484:8080 -it -e KUBERNETES_MASTER=http://$DOCKER_IP:8080 fabric8/hawtio

You can now access the web console at [http://192.168.59.103:8484/hawtio/kubernetes/pods](http://192.168.59.103:8484/hawtio/kubernetes/pods).

If you have setup the **dockerhost** alias in your /etc/hosts as described below you can use the simpler URL [http://dockerhost:8484/hawtio/](http://dockerhost:8484/hawtio/)

### Docker configuration if you are using a Mac, Windows or other platforms

Here's some tips on how to setup docker on your machine.

First we recommend you upgrade your boot2docker image so its the latest greatest.

    boot2docker download
    boot2docker up

If you are not on linux [this article](http://viget.com/extend/how-to-use-docker-on-os-x-the-missing-guide) describes how its a good idea to define **dockerhost** to point to your boot2docker ip address via:

    echo $(docker-ip) dockerhost | sudo tee -a /etc/hosts

Then you can access the REST API for OpenShift on the easier to remember and type URL: [http://dockerhost:8080/api/v1beta1/pods](http://dockerhost:8080/api/v1beta1/pods)

If you are using boot2docker 1.3.1, you should edit /var/lib/boot2docker/profile in boot2docker VM to disable TLS, so that can use 2375 as default DOCKER_HOST port and http connection for local registry.

    boot2docker ssh
    sudo vi /var/lib/boot2docker/profile

and add two lines

    DOCKER_TLS=no
    EXTRA_ARGS="--insecure-registry 192.168.59.103:5000"

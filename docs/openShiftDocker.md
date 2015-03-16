## Run OpenShift V3 using Docker

### Overview

There are two common approaches to running OpenShift with [Docker](https://docs.docker.com/):

* **Native Docker Install** - If you are using a Linux Operating System that natively suppors Docker such as Fedora, Centos or RHEL, then you can install and run OpenShift using a local Docker install.

* **Non-Native Docker Install** - If you are using an Operating System such as OSX or Windows that doesn't support Docker natively, you'll need to create a Virtual Machine to host Docker and subsequently OpenShift.

Even if your OS supports Docker natively it can be useful to use a Non-Native Install as you can then snapshot your Vm hosting OpenShift install in various states and restore.

Regardless of the approach taken you will need to:

* Setup your Docker environment (Prerequisites)
* Run the Start Script to install and configure OpenShift in Docker
* Configure your host to provide access to OpenShift (Env Variables and Network Routes)

---

### Prerequisites

You will need:

  * An Internet connection to download the Start script and pull docker images (docker images are then subsequently cached for offline use)
  * Sufficient disk space for your desired usage.  Docker images can be quite large and 20G will disappear quickly. Therefore, it's recommended to keep an eye on your available disk space.  There are some useful [clean up scripts](#clean-up-scripts) to help manage this.

#### Native Docker Install Pre Requisites

1. [Install Docker](https://docs.docker.com/installation/), the later the version generally the better it is!
2. Ensure you enable sudoless use of the docker daemon for users that will run the Fabric8 Start Script
3. Update your Docker config - this differs slightly on different Linux distros:

    On RHEL/Centos/Fedora:
    Copy the following line into `/etc/sysconfig/docker`

        OPTIONS="--selinux-enabled -H unix://var/run/docker.sock -H tcp://0.0.0.0:2375 --insecure-registry 172.0.0.0/8"

    If you are on Ubuntu/Debian:
    Copy the following line into `/etc/default/docker`

        DOCKER_OPTS="--selinux-enabled -H unix://var/run/docker.sock -H tcp://0.0.0.0:2375 --insecure-registry 172.0.0.0/8"
4. Restart the docker service

    `service docker restart`

5. If you are running on Fedora or other distro using `firewalld`, you will need to add `docker0` interface to the trusted zone like this:

    `firewall-cmd --zone=trusted --change-interface=docker0`
    `firewall-cmd --zone=trusted --change-interface=docker0 --permanent`

#### Non-Native Docker Install Pre Requisites

Traditionally Mac and Windows users would use [boot2docker](http://boot2docker.io/), a lightweight VM designed to make it feel like users can run docker natively.  This has been good in the past when working with a few containers however we have seen connectivity issues and problems related to low resources when working with more demanding technologies such as OpenShift & Kubernetes. Therefore, We __do not__ recommend you use boot2docker to run OpenShift.

Alternatively, you *could* create a VM manually using VMWare or [Virtual Box](https://www.virtualbox.org/) and install docker manually (using the  [Native Install Pre Requisites](#Native Docker Install Pre Requisites))

__Instead, we recommend that the simplest way to get going is to use the Fabric8 VagrantFile, as detailed below:__

1. Install the [Docker client](https://docs.docker.com/installation/) to interact with docker from your host machine, the later version the better! This is an optional step but recommended (Note that there isn't currently a binary distribution for Windows)
2. Install [Vagrant](http://www.vagrantup.com/downloads.html)
3. Check out the Fabric8 Git Repo or download a [repository snapshot zip](https://github.com/fabric8io/fabric8/archive/master.zip):

    `git clone git@github.com:fabric8io/fabric8.git`
4. Create a VM using the Fabric8 VagrantFile at the root of the repository:

    `cd fabric8`
    `vagrant up`
    `vagrant ssh`

   Note: There are alternative Vagrant images available in the [Fabric8 Repo](https://github.com/fabric8io/fabric8/tree/master/support/vagrant)

5. We recommend priming the docker registry with the images used by Fabric8, and then using Vagrant to take a snapshot so we can revert back to a clean start without re-downloading gigabites of docker images.

    first install the snapshot plugin

    `vagrant plugin install vagrant-vbox-snapshot`

    now prime the registry

    `bash <(curl -sSL https://bit.ly/get-fabric8) -p`

    or if you want to pull down all the docker images for the kitchen sink

    `bash <(curl -sSL https://bit.ly/get-fabric8) -pk`

    take a snapshot of the vagrant image:

    `exit`
    `vagrant snapshot take default cleanstart`

    __Now at any point you can reset to the cleanstart snapshot via:__

    `vagrant snapshot go default cleanstart`
---

### Run the Start Script

We use a script downloaded via curl to configure and start OpenShift in a Docker container. This script will then schedule a number of further containers such as the Fabric8 console and optionally logging and metric dashboards in the form of [Kibana](http://www.elasticsearch.org/overview/kibana) and [Grafana](http://play.grafana.org/#/dashboard/db/grafana-play-home).

Once you've run the script you'll need to complete some post-install steps before you can access Fabric8.

__The first time you run fabric8 v2 it may take some time as there are a number of docker images to download, this may require some patience__

If you are running fabric8 v2 for the first time we recommend you start with the basic script. This should be run from the machine hosting Docker.

#### Basic

If you fancy starting OpenShift V3 the super-easy way which includes the [Fabric8 console](console.html) and a [Private Docker registry](https://registry.hub.docker.com/_/registry/), run this one-liner.

    bash <(curl -sSL https://bit.ly/get-fabric8)

If using a native install a Browser will automatically open on completion showing the Fabric8 console

#### Full

If you would like to try out a fully-featured installation, including aggregated logs & metrics, you can pass in the `-k` ("kitchen sink") flag

__warning: more images to download so longer to wait__

    bash <(curl -sSL https://bit.ly/get-fabric8) -k

#### Recreate

If you want to start from scratch, deleting all previously created Kubernetes containers, you can pass in the `-f` flag:

    bash <(curl -sSL https://bit.ly/get-fabric8) -f

#### Update

To update all the relevant images (corresponds to docker pull latest docker images), you can pass the `-u` flag:

    bash <(curl -sSL https://bit.ly/get-fabric8) -u

#### Combination

And of course flags can be combined, to start from scratch & update all images at once:

    bash <(curl -sSL https://bit.ly/get-fabric8) -kuf

---

### Post Install Steps

#### Set Environment variables

You'll need to set the following environment variables on your host to be able use the [Tools](http://fabric8.io/v2/tools.html) such as the [Console](console.html), [Maven Plugin](http://fabric8.io/v2/mavenPlugin.html), the [Forge Addons](http://fabric8.io/v2/forge.html) and the [java libraries](javaLibraries.html):

    FABRIC8_CONSOLE - used to interact with the faberic8 console to deploy and run apps
    DOCKER_REGISTRY  - used to push images to the private docker registry
    KUBERNETES_TRUST_CERT
    DOCKER_IP - used by the docker client to connect to docker
    DOCKER_HOST
    KUBERNETES_MASTER  - used to interact with the Kubernetes Rest API

These environment variables are presented to you on succesfull completion of the start scirpt, so the easiest thing to do is copy them from the output into your ~/.bashrc (linux) or ~/.profile (mac). Windows users will need to set them individually via the Environment Variables dialog.

#### Setup Network Routes (Non-Native Install Only)

To make sure you can access the IP addresses of services and pods hosted in your Virtual Machine (e.g. the Fabric8 Console), you'll need to add a network route for the 172.X.X.X IP range:

    sudo route -n delete 172.0.0.0/8
    sudo route -n add 172.0.0.0/8 $DOCKER_IP

Or on Windows run the following from a DOS prompt as an Administrator:

    route add 172.0.0.0 MASK 255.0.0.0 $DOCKER_IP

You should now be able to access the Fabric8 console using the address defined in the previously set $FABRIC8_CONSOLE environment variable.

#### Using the osc command line

The OpenShift container includes the OpenShift Command Line (osc). Therefore, you can use the osc via a docker 'run' command.

The easiest way to do this is to create an alias in your ~/.bashrc.

**Note:** In the aliases below you'll need to update the openshift/origin container tag to be whatever version Fabric8 is currently using. Run `docker ps | grep openshift/origin:` to find this out

**Native Install**

    alias osc="docker run --rm -i --entrypoint=osc --net=host openshift/origin:v0.3.4 --insecure-skip-tls-verify"

**Non-Native Install**

    alias osc="docker run --rm -i -e KUBERNETES_MASTER=https://$DOCKER_IP:8443 --entrypoint=osc --net=host openshift/origin:v0.3.4 --insecure-skip-tls-verify"

You can now use the osc command line to list pods, replication controllers and services; delete or create resources etc:

    osc get pods
    osc get replicationControllers
    osc get services

To see all the available commands:

    osc --help

**Note** that since we are using docker and you are typically running the docker commands from the host machine (your laptop), the osc command which itself runs in a linux container won't be able to access local files when supplying -c to create.

However you can pipe them into the command line via

    cat mything.json | osc create -f -
---

### Miscellaneous

#### Restarting OpenShift

If docker is restarted, you can restart openshift by starting the openshift container

    docker start openshift

#### Clean up script

To remove untagged images

    sudo docker rmi $(sudo docker images -qaf 'dangling=true')

Sometimes Docker containers are not removed completely.  The lines below will stop and remove OpenShift and all containers created by Kubernetes.

    docker kill openshift
    docker rm -v openshift
    docker kill $(docker ps -a | grep k8s | cut -c 1-12)
    docker rm -v $(docker ps -a -q)

#### Running a Hawtio Console

If you are developing and working with hawtio you might want to run a locally built hawtio docker image against OpenShift..

    docker run -p 8484:8080 -it -e KUBERNETES_MASTER=https://$DOCKER_IP:8443 fabric8/hawtio

You can now access the web console at http://$DOCKER_IP:8484/hawtio/kubernetes/pods.

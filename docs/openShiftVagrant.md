## Run OpenShift V3 using Vagrant

Lets run OpenShift V3 using Vagrant. Be sure to check the latest [OpenShift docs](https://github.com/openshift/origin/blob/master/CONTRIBUTING.adoc#develop-on-virtual-machine-using-vagrant) just in case!.

* You will need to [install vagrant](https://www.vagrantup.com/downloads.html) if you have not done so already
* clone the [OpenShift Origin repository](https://github.com/openshift/origin)

```
  git clone https://github.com/openshift/origin.git openshift-origin
  cd openshift-origin
```

* vagrant up:

```
export OPENSHIFT_DEV_CLUSTER=true
export OPENSHIFT_NUM_MINIONS=1
vagrant up
```

You now should have a master node running on **10.245.1.2**.

You can test this by querying the REST API for the pods at: [http://10.245.1.2:8080/api/v1beta1/pods](http://10.245.1.2:8080/api/v1beta1/pods). It should return some valid JSON (but be mostly empty).

## Setup your machine

There's a few things that will make it easier for you to work with OpenShift on vagrant on your host machine:

First you'll need to [install docker](https://docs.docker.com/installation/), the later the version generally the better it is!

### If you are using a Mac, Windows or other platforms

First we recommend you upgrade your boot2docker image so its the latest greatest.

    boot2docker download
    boot2docker up

If you are using boot2docker 1.3.1, you should edit /var/lib/boot2docker/profile in boot2docker VM to disable TLS, so that can use 2375 as default DOCKER_HOST port and http connection for local registry.

    boot2docker ssh
    sudo vi /var/lib/boot2docker/profile

and add two lines

    DOCKER_TLS=no
    EXTRA_ARGS="--insecure-registry 192.168.59.103:5000 --insecure-registry dockerhost:5000 --insecure-registry 10.245.2.2:5000 --insecure-registry 10.245.2.3:5000"
    ### Setting up your environment variables to use OpenShift

Add the following to your ~/.profile or ~./bashrc

    export KUBERNETES_MASTER=http://10.245.1.2:8080

You now should be able to use the various [Tools](http://fabric8.io/v2/tools.html) such as the [Console](console.html), [Maven Plugin](http://fabric8.io/v2/mavenPlugin.html), the [Forge Addons](http://fabric8.io/v2/forge.html) and the [java libraries](javaLibraries.html) to work on your local OpenShift.


### Adding the master and minion hosts to /etc/hosts

You might find it easier working on your host machine and interacting with the master and minions by adding this to your **/etc/hosts**

    10.245.1.2 openshift-master
    10.245.2.2 openshift-minion-1
    10.245.2.3 openshift-minion-2

### Network routes

Add a network route so you can connect to pods from your host

    sudo route -n add 10.244.1.0/24 10.245.2.2
    sudo route -n add 10.244.2.0/24 10.245.2.3

## Tips on working with OpenShift on Vagrant

Here's a bunch of handy tips:

### Starting OpenShift

If for any reason OpenShift stops running you can start it again via:

    vagrant ssh master
    sudo systemctl start openshift-master.service

    vagrant ssh minion-1
    sudo systemctl start openshift-node.service

    vagrant ssh minion-2
    sudo systemctl start openshift-node.service

### Recreating OpenShift

If you suspend your VMs or reboot your boxes; or just want to reset your OpenShift installation to a clean installation, just run this from your openshift-origin folder:

    cd openshift-origin
    vagrant-restart-openshift.sh

which will reload your VMs, delete any temporary volumes and start up the services.

Note you must have the **fabric8/bin** folder on your PATH to find the [vagrant-restart-openshift.sh script](https://github.com/fabric8io/fabric8/blob/master/bin/vagrant-restart-openshift.sh)


### OpenShift logs

To view the openshift logs in the master or minions use:

    journalctl -f

### Running hawtio via docker

If you wish you can run a docker hawtio console via:

    docker pull fabric8/hawtio
    docker run -p 9282:8080 -it -e KUBERNETES_MASTER=$KUBERNETES_MASTER -e DOCKER_HOST=$DOCKER_HOST fabric8/hawtio

### Working with OpenShift in a VM

There are a few things we need to be aware of if running OpenShift in a VM and developing on our host.

When pushing images make sure you have set the DOCKER_REGISTRY env var to the correct minion that is hosting the registry, for example..

	export DOCKER_REGISTRY=10.245.2.2:5000

If you are running a non Linux host and using boot2docker then you still need to follow the [Setup Machine](setupMachine.md) guide and set DOCKER_HOST so we can run docker commands (like docker push) on our host as usual ..

	export DOCKER_HOST=tcp://192.168.59.103:2375

### Checking your $DOCKER_REGISTRY

There's a handy script called  [ping-registry.sh](https://github.com/fabric8io/fabric8/blob/master/bin/ping-registry.sh) which will check that you have your **DOCKER_REGISTRY** environment variable setup correctly to point to a valid docker registry so that you can create and push docker images:

    ping-registry.sh



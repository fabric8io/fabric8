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

### Setting up your environment variables to use OpenShift

Add the following to your ~/.profile or ~./bashrc

    export KUBERNETES_MASTER=http://10.245.1.2:8080

You now should be able to use the various [Tools](http://fabric8.io/v2/tools.html) such as the [Web Console](console.html), [Maven Plugin](http://fabric8.io/v2/mavenPlugin.html), the [Forge Addons](http://fabric8.io/v2/forge.html) and the [java libraries](javaLibraries.html) to work on your local OpenShift.


### Adding the master and minion hosts to /etc/hosts

You might find it easier working on your host machine and interacting with the master and minions by adding this to your **/etc/hosts**

    10.245.1.2 openshift-master
    10.245.2.2 openshift-minion-1
    10.245.2.3 openshift-minion-2

### Re-starting OpenShift

If for any reason OpenShift stops running you can restart it via:

    vagrant ssh master
    sudo systemctl start openshift-master.service

### OpenShift logs

To view the openshift logs in the master or minions use:

    journalctl -f

### Running hawtio via docker

If you wish you can run a docker hawtio console via:

    docker pull fabric8/hawtio
    docker run -p 9282:8080 -it -e KUBERNETES_MASTER=$KUBERNETES_MASTER -e DOCKER_HOST=$DOCKER_HOST fabric8/hawtio



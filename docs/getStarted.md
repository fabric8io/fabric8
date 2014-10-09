## Getting Started

In order to start working with the Fabric8 on your laptop, you need to have 
[Kubernetes](https://github.com/GoogleCloudPlatform/kubernetes) Kube and 
[etcd](https://github.com/coreos/etcd) services available. The quickest way to start Kube and etcd locally 
is to run OpenShift V3 on your machine.

### Get OpenShift V3

You can download a [distribution of OpenShift V3](https://github.com/openshift/origin/releases) if there is a download 
for your platform (currently OpenShift runs only on the 64-bit Linux). We recommend to download the [alpha nightly 20141003/e4d4ecf]
(https://github.com/openshift/origin/releases/download/20141003/openshift-origin-linux64-e4d4ecf.tar.gz) version of the
OpenShift 3 - Fabric8 has been tested against it.

Or you can build it yourself:

* install [go lang](http://golang.org/doc/install)
* [compile the OpenShift V3 code](https://github.com/jstrachan/origin/blob/master/README.md#getting-started) via this command:

    hack/build-go.sh

Then add _output/go/bin to your PATH.

### Setting up your machine

To simplify the documentation and configuration script for fabric8 we are going to setup 2 local host aliases, **openshifthost** and **dockerhost** then all the instructions that follow on this site will work whether you use Docker natively on linux or you run docker in a VM or you are on a Mac or Windows and are using boot2docker.

Strictly speaking this isn't required to run fabric8 and OpenShift. e.g. on a linux box with native docker you could replace _dockerhost_ and _openshifthost_ with localhost in all the instructions and in the app/fabric8.json file :)

However doing this will make it easier to document; so if you are trying fabric8 on your laptop this is currently the simplest approach.

##### Using linux with a native docker:

If you are on linux try this to define the 2 hosts to point to your localhost:

    export OPENSHIFT_HOST=127.0.0.1
    echo $OPENSHIFT_HOST openshifthost | sudo tee -a /etc/hosts
    echo $OPENSHIFT_HOST dockerhost | sudo tee -a /etc/hosts

Another option is to replace dockerhost and openshifthost in the fabric8/apps/fabric8.json file with "localhost" and use "localhost" in the 2 environment variables above and then use "localhost" whenever the documentation mentions "dockerhost" or "openshifthost".

##### Using other platforms

When there is not a native docker, such as on a Mac or Windows when using boot2docker, there will be different IP addresses for your host machine and the boot2docker VM. So we are going to setup 2 aliases for the host (openshifthost) and docker (dockerhost):

First lets define an environment variable to point to your local IP address. You may already know this.

If not depending on your network settings and whether you are on ethernet or wifi either this:

    export OPENSHIFT_HOST=`ipconfig getifaddr en0`
    echo $OPENSHIFT_HOST

should print out your IP address or if not then try this:

    export OPENSHIFT_HOST=`ipconfig getifaddr en1`
    echo $OPENSHIFT_HOST

If you still don't have an IP address in the **OPENSHIFT_HOST** environment variable, try just running:

    ifconfig

and seeing if you can spot one. If not try figure it out yourself; e.g. look in your operating system settings.

Now lets setup the **openshifthost** alias:

    echo $OPENSHIFT_HOST openshifthost  | sudo tee -a /etc/hosts

Now lets setup the **dockerhost** alias that should point to the ip address of boot2docker:

    echo `boot2docker ip 2> /dev/null` dockerhost | sudo tee -a /etc/hosts

#### Testing your setup

You should be able to ping the 2 host names:

    ping dockerhost
    ping openshifthost

If you are on a Mac or Windows the above should also work if you ssh into boot2docker via

    boot2docker ssh

#### Environment variables

Now lets set up some environment variables:

    export KUBERNETES_MASTER=http://openshifthost:8080
    export DOCKER_REGISTRY=dockerhost:5000


### Start OpenShift

Then you can start it up:

    $ openshift start --listenAddr=$OPENSHIFT_HOST:8080

You can then use the OpenShift command line tool or the OpenShift console.


### Running fabric8

To make sure you've got the latest and greatest hawtio console try this first:

    docker pull fabric8/hawtio

Now to install fabric8 try:

    cd fabric8/apps
    openshift kube apply -c fabric8.json

This will run a local docker registry and the hawtio web console.

You should be able to check if the docker registry is running OK via this command (which should return 'true'):

    curl http://$DOCKER_REGISTRY/v1/_ping

#### Accessing the web console

You should be able to access the web console on your docker host at port 9282 via [http://dockerhost:8282/hawtio/kubernetes/pods](http://dockerhost:8282/hawtio/kubernetes/pods)

If you want to hack on the code you can [run a local build of hawtio](https://github.com/hawtio/hawtio/blob/master/BUILDING.md#running-hawtio-against-kubernetes--openshift)


#### If you are on a Mac

You may not be able to ping the pod IP addresses when you've created pods. You can use the following command to be able to network from your host OS to the Pod IP addresses inside boot2docker:

    sudo route -n add  172.17.0.0/16 192.168.59.103

If you want to also be able to access POD IP ports from your Host operating system them you may also want to run the following in **boot2docker**

    sudo iptables -P FORWARD     ACCEPT

Now any Pod IP and port should be accessible both from your Host and also within the boot2docker vm

If you need more help check this guide on [iptables](https://www.frozentux.net/iptables-tutorial/iptables-tutorial.html) and [four ways to connect a docker container to a local network](http://blog.oddbit.com/2014/08/11/four-ways-to-connect-a-docker/)

### Whats next?

So you should have now an empty OpenShift environment with the core of fabric8 installed (the web console and a local docker registry).

Now you could try:

 * [deploying an example quickstart project](http://fabric8.io/v2/mavenPlugin.html#example)

### Other Resources

#### View a demo

To help you get started, you could watch one of the demos in the  <a class="btn btn-success" href="https://vimeo.com/album/2635012">JBoss Fuse and JBoss A-MQ demo album</a>

For example, try the <a class="btn btn-success" href="https://vimeo.com/80625940">JBoss Fuse 6.1 Demo</a>

#### Try QuickStarts

New users to Fabric8 should try the [QuickStarts](/gitbook/quickstarts.html).

#### Read the documentation

Check out the [Overview](/gitbook/overview.html) and [User Guide](/gitbook/index.html).

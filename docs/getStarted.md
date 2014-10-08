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

### Running OpenShift V3

Point your environment at the local kube master:

    export KUBERNETES_MASTER=http://127.0.0.1:8080

Then you can start it up:

    $ openshift start

You can then use the OpenShift command line tool; the REST API, the OpenShift console or hawtio to work with it.

#### Running a local docker registry

If you want to create your own images for your OpenShift environment run

    cd fabric8/apps
    openshift kube apply -c registry-config.json

You will then get a docker registry running on http://localhost:5000. You can check if its up and working via http://localhost:5000/v1/_ping

### Running a local build of hawtio

If you want to hack on the code you can [run a local build of hawtio](https://github.com/hawtio/hawtio/blob/master/BUILDING.md#running-hawtio-against-kubernetes--openshift)

#### If you are on a Mac

You may not be able to ping the pod IP addresses when you've created pods. You can use the following command to be able to network from your host OS to the Pod IP addresses inside boot2docker:

    sudo route -n add  172.17.0.0/16 192.168.59.103

If you want to also be able to access POD IP ports from your Host operating system them you may also want to run the following in **boot2docker**

    sudo iptables -P FORWARD     ACCEPT

Now any Pod IP and port should be accessible both from your Host and also within the boot2docker vm

If you need more help check this guide on [iptables](https://www.frozentux.net/iptables-tutorial/iptables-tutorial.html) and [four ways to connect a docker container to a local network](http://blog.oddbit.com/2014/08/11/four-ways-to-connect-a-docker/)

### Other Resources

#### View a demo

To help you get started, you could watch one of the demos in the  <a class="btn btn-success" href="https://vimeo.com/album/2635012">JBoss Fuse and JBoss A-MQ demo album</a>

For example, try the <a class="btn btn-success" href="https://vimeo.com/80625940">JBoss Fuse 6.1 Demo</a>

#### Try QuickStarts

New users to Fabric8 should try the [QuickStarts](/gitbook/quickstarts.html).

#### Read the documentation

Check out the [Overview](/gitbook/overview.html) and [User Guide](/gitbook/index.html).

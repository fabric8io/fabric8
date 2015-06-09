## Setup the OpenShift client locally

This page describes how to setup your local development machine or host to work with Kubernetes or OpenShift either remotely or via [fabric8 vagrant image](openShiftWithFabric8Vagrant.html).

### Install the OpenShift binaries to your $PATH

* download and unpack a [release of OpenShift 0.5.1 or later](https://github.com/openshift/origin/releases/) for your platform and add the `openshift`, `osc` and `osadm` executables to your `PATH`
* login to OpenShift

```
osc login --server=https://172.28.128.4:8443
```

You should now be able to use the [osc CLI tool](https://github.com/openshift/origin/blob/master/docs/cli.md) to work with Kubernetes and OpenShift resources:

```
osc get pods
```

### Environment variables

The following environment variables are useful. The following are for the [fabric8 vagrant image](openShiftWithFabric8Vagrant.html); if you are running on a different box just update the IP / domain names:

    export KUBERNETES_MASTER=https://172.28.128.4:8443
    export KUBERNETES_DOMAIN=vagrant.local
    export KUBERNETES_NAMESPACE=default

#### Adding entries in /etc/hosts

This step is optional but if you are running the [fabric8 vagrant image](openShiftWithFabric8Vagrant.html) and don't have a wildcard DNS entry setup for `*.vagrant.local` then you might want to add an entry in your `/etc/hosts` file so that you can access services via their routes.

		172.28.128.4 vagrant.local fabric8.vagrant.local fabric8-master.vagrant.local docker-registry.vagrant.local gogs.vagrant.local nexus.vagrant.local jenkins.vagrant.local

Where `172.28.128.4` is the IP address of your OpenShift master (which it is for the [fabric8 vagrant image](openShiftWithFabric8Vagrant.html))

e.g. if you [Install Fabric8 on OpenShift](fabric8OnOpenShift.html) then you should be able to access the console at [http://fabric8.vagrant.local/](http://fabric8.vagrant.local/)

### Reuse Docker from Vagrant

If you are using a vagrant image such as the [Fabric8 vagrant image](openShiftWithFabric8Vagrant.html) then you should setup your host to reuse the same docker daemon that is inside the vagrant image; this lets you [develop locally without having to push or pull images to or from a docker registry](developLocally.html) which greatly speeds up local development.

So set this environment variable:

    export DOCKER_HOST=tcp://vagrant.local:2375

And don't define any of the other docker env vars like `DOCKER_CERT_PATH` or `DOCKER_TLS_VERIFY`.

Now when in a shell on your host (OS X / Windows) you can type `docker ps` and the output is the same as if you type the same command inside the `vagrant ssh` shell inside the vagrant VM.

This then means that your host (OS X / Windows) will use the same docker daemon as the Kubernetes running inside the fabric8 vagrant image. Because of this you don't need to use a docker registry; a docker daemon is kinda like a local registry anyway - provided you don't try to push or pull the image! 

**NOTE**  you need to make sure that your kubernetes JSON uses the imagePullPolicy of `PullIfAbsent` (via the maven property `fabric8.imagePullPolicy`); as any attempt to pull an image just built locally that isn't pushed to a docker registry won't work; since the image isn't public yet. In fabric8 2.1.11 the default in `mvn fabric8:json` is to now omit this value which means it uses the kubernetes default which is `PullIfAbsent`.

You now should be able to [build and use docker images locally without pushing or pulling](developLocally.html).

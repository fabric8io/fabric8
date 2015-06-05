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

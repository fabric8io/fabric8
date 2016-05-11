## Getting Started

### OpenShift / Kubernetes setup

Before you start using fabric8 you will need a Kubernetes or Kubernetes cluster. Here are some recipies:

* [**Vagrant**](vagrant.html) - OpenShift Origin

  This is the easiest and fastest way to get started with fabric8 locally.
  All you need is [Vagrant](https://www.vagrantup.com/) and [VirtualBox](https://www.virtualbox.org/) installed locally.
  The detailed steps are described in "[Fabric8 Vagrant Image](vagrant.html)"

* [**Native OpenShift**](openshift.html) - OpenShift Origin

  If you have [OpenShift V3](http://www.openshift.com) installed locally, this [guide](openshift.html) helps you step-by-step how to install fabric8 on it.

* [**Google Container Engine**](gke.html) - Kubernetes

  [Google Container Engine](https://cloud.google.com/container-engine/) (GKE) is a Google hosted Kubernetes platform. There are many other ways to install Kubernetes as listed on the Kubernetes [Getting Started](http://kubernetes.io/gettingstarted/) page however if you want to get up and running quickly without having to setup infrastructure and run through installations then GKE is a great option.  This [guide](gke.html) will help you step-by-step install fabric8 using vanilla Kubernetes hosted on GKE.


#### Local client setup

For a smooth developer experience and for using the fabric8 [tools](../tools.html) a local OpenShift client needs to be setup.
How this is done is described in this [recipe](local.html).

### What's next

* [Run a fabric8 App](apps.html)
* [Check out the Quickstarts](../quickstarts/index.html)
* [Learn how to develop applications locally](develop.html)
* [Running a quickstart on the iPaaS](example.html)

### Troubleshooting

Hopefully that will get you started quickly; if you hit any issues check out the [FAQ](http://fabric8.io/guide/FAQ.html), [get in touch](http://fabric8.io/community/index.html) or [raise an issue](https://github.com/fabric8io/fabric8/issues)

Also check out the [troubleshooting guide](troubleshooting.html)

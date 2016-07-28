## Getting Started

### OpenShift / Kubernetes setup

Before you start using fabric8 you will need a Kubernetes or OpenShift cluster. Here are some recipes:

#### Already have a working Kubernetes cluster?

If you already have a cluster installed try this:

* [**Kubernetes**](kubernetes.html): If you have a working [Kubernetes](http://kuberentes.io) cluster use [this guide to install fabric8](kubernetes.html)

* [**OpenShift**](openshift.html): If you have a working [OpenShift V3](http://www.openshift.com) cluster use [this guide to install fabric8](openshift.html)


#### Don't have a Kubernetes cluster yet?

* [**MiniKube**](minikube.html)

  This is the easiest and fastest way to get started with Fabric8 and Kubernetes locally.
  You just need to download 2 small binaries ([minikube](https://github.com/kubernetes/minikube/releases) and [gofabric8](https://github.com/fabric8io/gofabric8/releases)) - no need for VirtualBox or Vagrant!
  Check out how to [Install Fabric8 on a MiniKube created cluster of Kuberetes](minikube.html)

* [**MiniShift**](minishift.html)

  This is the easiest and fastest way to get started with Fabric8 and OpenShift Origin locally.
  You just need to download 2 small binaries ([minishift](https://github.com/jimmidyson/minishift/releases) and [gofabric8](https://github.com/fabric8io/gofabric8/releases)) - no need for VirtualBox or Vagrant!
  Check out how to [Install Fabric8 on a MiniShift created cluster of OpenShift Origin](minishift.html)

* [**Vagrant with Kubernetes**](vagrant-kubernetes.html)

  If you already have Vagrant and VirtualBox installed and want to use it to work with Fabric8 and Kubernetes locally then this option is for you.
  All you need is [Vagrant](https://www.vagrantup.com/) and [VirtualBox](https://www.virtualbox.org/) installed locally.
  Check out how to [Create the Fabric8 Vagrant Image for Kubernetes](vagrant-kubernetes.html)

* [**Vagrant with OpenShift Origin**](vagrant.html)

  If you already have Vagrant and VirtualBox installed and want to use it to work with Fabric8 and OpenShift Origin locally then this option is for you.
  All you need is [Vagrant](https://www.vagrantup.com/) and [VirtualBox](https://www.virtualbox.org/) installed locally.
  Check out how to [Create the Fabric8 Vagrant Image for OpenShift Origin](vagrant.html)

* [**CDK**](cdk.html) - Using the Red Hat Container Development Kit

  You will need is [Vagrant](https://www.vagrantup.com/) and [VirtualBox](https://www.virtualbox.org/) installed locally along with a [number of prerequisites](https://github.com/redhat-developer-tooling/openshift-vagrant#prerequisites).
  Check out [Installing Fabric8 inside the CDK Vagrant Image](cdk.html)

#### Use the public cloud?

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

## Setup OpenShift

There are a few ways you can install and setup [OpenShift V3](http://www.openshift.org/)

### Using Vagrant

The easiest way to work with fabric8 and OpenShift is via the fabric8 vagrant image:

* [Fabric8 Vagrant Image](openShiftWithFabric8Vagrant.html)

### Native Linux installation

If you are on Linux and want to install OpenShift without Vagrant then follow these instructions:

* [Use the OpenShift Origin Installation documentation](http://docs.openshift.org/latest/getting_started/developers.html)

Here are our additional [tips on installing OpenShift on Linux](openShiftInstall.html) from the release distro.

Somme additional [requirements](openShiftInstall.html#configure-openshift) are needed for running the [fabric8 applications](fabric8OnOpenShift.html)

### Setup the OpenShift client on your local machine

Whether you run OpenShift locally, remotely or via the [fabric8 vagrant box](openShiftWithFabric8Vagrant.html) you'll need to:

* [Setup the OpenShift clients on your local machine to work with Kubernetes and OpenShift](setupLocalHost.html)

### Other approaches

If you are not on Linux then you could try using OpenShift's vagrant image:

* [Start OpenShift V3 using OpenShift's Vagrant image](openShiftVagrant.html)

For 2.0.x releases we used to recommend using a bash script with docker:

* [Start OpenShift V3 using Docker](openShiftDocker.html)

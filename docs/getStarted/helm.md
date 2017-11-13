## Helm

[Helm](https://github.com/kubernetes/helm/) is a package manager for Kubernetes. It allows you to package and release applications as tarballs (called _charts_) into a _versioned repository_ and then lets you install, upgrade or remove the apps.

So helm is a great way to install fabric8 on an existing kubernetes cluster and then later on upgrade it when a new release of fabric8 comes out.

**NOTE** currently helm does not properly support OpenShift since OpenShift uses lots of non-standard resource kinds - so for now we recommend not using Helm for OpenShift -instead we recommend using the [gofabric8 installer](gofabric8.html) for OpenShift. We do hope long term to have great helm support for OpenShift though - as we view it as essential technology for managing upgrades of software.


### Installing fabric8 with helm

You can install the fabric8 platform using helm via the following:

### Setup helm

Before installing any helm chart you need to setup Helm

* download a [helm binary for your platform](https://github.com/kubernetes/helm/#install)
* then type:

```bash
helm init
```

this will initialise helm on your kubernetes cluster

### Add the fabric8 repository

We maintain helm charts for all the microservices in the fabric8 ecosystem and store them in the [fabric8 helm chart repository](http://fabric8.io/helm/).

To add the fabric8 repository to your helm installation type:

```bash
helm repo add fabric8 https://fabric8.io/helm
```

### Install the fabric8 platform

```bash
helm install fabric8/fabric8-platform
```

### Query all other fabric8 charts

```bash
helm search fabric8
```

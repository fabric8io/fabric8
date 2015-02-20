## Overview

Fabric8 provides an integration and management pltform above the [Kubernetes Platform](http://kubernetes.io).

So Fabric8 works great with [Docker](http://www.docker.com/) and implementations of Kubernetes such as [OpenShift V3](http://openshift.github.io/), [Project Atomic](http://www.projectatomic.io/) and [Google Container Engine](https://cloud.google.com/container-engine/).

For non-linux platforms which don't yet support native Docker we have [Jube](jube.html) which is an open source pure Java implementation of Kubernetes.

Kubernetes is supported on Google and Microsofts clouds, by OpenShift V3 (on premise and public cloud), by Project Atomic and VMware; so it's increasingly becoming the standard API to PaaS and _Container As A Service_ on the open hybrid clouds. [Jube](jube.html) then helps extend Kubernetes to run Java based middleware on any operating system which supports Java 7.

#### How it all fits together

* Docker provides the abstraction for packaging and creating Linux based lightweight containers
* Kubernetes provides the mechanism for orchestrating docker containers on multiple hosts (like a distributed systemd) together with networking them together
* Kubernetes extensions provides the packaging, templating and building mechanisms

#### Kubernetes model

* [Pods](pods.html)
* [Replication Controllers](replicationControllers.html)
* [Services](services.html)

#### Kubernetes extensions from OpenShift Origin v3

* [Apps (or Kubernetes Application Templates)](apps.html)
* [Builds](builds.html)

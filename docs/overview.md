## Overview

Fabric8 2.x provides an integration platform above [Docker](http://docker.io/), [Kubernetes](http://kubernetes.io) and the [Kubernetes extensions defined in OpenShift Origin](). For non-linux platforms which don't support docker we have a [Docker Emulation Layer](emulation.html).

Kubernetes is supported on Google & Microsofts clouds, by OpenShift v3 (on premise and public cloud), by Project Atomic and VMware; so it's increasingly becoming the standard API to PaaS and Container As A Service on the hybrid clouds. So it makes sense for fabric8 to be optimised to run well on top of Kubernetes so it can reuse Container As A Service across the open hybrid cloud.

#### How it all fits together

* Docker provides the abstraction for packaging and creating Linux based lightweight containers
* Kubernetes provides the mechanism for running docker containers on multiple hosts (like a distributed systemd) together with networking them together
* Kubernetes extensions provides the packaging, templating and building mechanisms

#### Kubernetes model

* [Pods](pods.html)
* [Replication Controllers](replicationControllers.html)
* [Services](services.md)

#### Kubernetes extensions from OpenShift Origin v3

* [Apps (or Kubernetes Application Templates)](apps.html)
* [Builds](builds.html)

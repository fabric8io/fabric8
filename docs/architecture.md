## Architecture

Fabric8 provides a number of microservices and tools above the [Kubernetes](http://kubernetes.io) cloud.

* Docker provides the abstraction for packaging and creating Linux based lightweight containers
* [Kubernetes](http://kubernetes.io) provides the mechanism for orchestrating docker containers on multiple hosts (like a distributed systemd) together with networking them together
* Kubernetes extensions provides the packaging, templating and building mechanisms

#### Kubernetes model

* [Pods](pods.html)
* [Replication Controllers](replicationControllers.html)
* [Services](services.html)
* [Environments](environments.html)
* [Apps](apps.html)

#### Kubernetes extensions from OpenShift Origin v3

* [OpenShift templates](http://docs.openshift.org/latest/dev_guide/templates.html) 
* [Builds](builds.html)

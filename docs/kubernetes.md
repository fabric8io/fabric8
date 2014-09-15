## Kubernetes Containers

The Kuberetes container provider uses the [Kuberetes API](http://kubernetes.io/) for creating and deleting containers.

It can be enabled by adding the **kubernetes** profile or specifying the **FABRIC8_PROFILES** environment variable before you create a fabric:

    export FABRIC8_PROFILES=kubernetes

### Prerequisits

You need to specify the following environment variables; in particular you must specify the **KUBERNETES_MASTER** URL for connecting to the kubernetes REST API

    export KUBERNETES_MASTER=http://127.0.0.1:8080/
    export FABRIC8_PROFILES=kubernetes

### Working with kubernetes

If you are already familiar with fabric8 then using fabric8 and kubernetes appears like the [docker support](http://fabric8.io/gitbook/docker.html); the main differences are

* kubernetes uses multiple hosts to provision containers
* each container (in fabric8 terminology) maps to a _pod_ in kubernetes terminology which consists of usually 1 docker container.
* each pod in kubernetes gets its own IP address

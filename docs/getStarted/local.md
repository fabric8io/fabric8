## Setup the OpenShift client locally

This page describes how to setup your local development machine or host to work with Kubernetes or 
OpenShift either remotely or via [fabric8 vagrant image](vagrant.html). If your running OpenShift natively on 
your localhost, you should be already setup properly.

### Install the OpenShift binaries to your $PATH

* Download and unpack a [release of OpenShift 0.5.1 or later](https://github.com/openshift/origin/releases/) 
  for your platform and add the `openshift`, `oc` and `oadm` executables to your `PATH`
* Login to OpenShift

```
oc login --server=https://172.28.128.4:8443
```

You should now be able to use the [oc CLI tool](https://github.com/openshift/origin/blob/master/docs/cli.md) 
to work with Kubernetes and OpenShift resources:

```
oc get pods
```

### Environment variables

Set the following environment variables.

 - Unix flavored OSs:
  
          export KUBERNETES_MASTER=https://172.28.128.4:8443
          export KUBERNETES_DOMAIN=vagrant.f8
          export DOCKER_HOST=tcp://vagrant.f8:2375

  - Windows:

          set KUBERNETES_MASTER=https://172.28.128.4:8443
          set KUBERNETES_DOMAIN=vagrant.f8
          set DOCKER_HOST=tcp://vagrant.f8:2375

The domain name of course varies depending on you initial setup. This example assumes that you are using the 
[Vagrant image](vagrant.md).

It is probably a good idea to add this into your `~/.profile` (Linux, OS X) or 
*System* -> *Advanced System Settings* -> *Environment Variables* (Windows)

#### Adding entries in /etc/hosts

For each fabric8 application to access from externally you need a route setup in `/etc/hosts`. If you are 
using the fabric8 [vagrant image](vagrant.html) this can be automated. 
If you are using [OpenShift natively](openshift.html) then 
you need to update your `/etc/hosts` (for Windows: `%WINDIR%\System32\drivers\etc\hosts`) 
so that you can access services via their routes: 

		10.0.2.2 vagrant.f8 fabric8.vagrant.f8 docker-registry.vagrant.f8 gogs.vagrant.f8 
		10.0.2.2 nexus.vagrant.f8 jenkins.vagrant.f8

Where `10.0.2.2` is the IP address of your OpenShift master. If you get an error when accessing an fabric8 service, please
check that your service is added in the hosts file.

Alternatively you can setup a `*.vagrant.f8` wildcard DNS entry. How to do this is depends on your OS and is beyond the 
scope of this document.

### Reuse Docker from Vagrant

If you are using a vagrant image such as the [Fabric8 vagrant image](vagrant.html) then you should setup 
your host to reuse the same docker daemon that is inside the vagrant image; 
this lets you develop locally without having to push or pull images to or from a docker registry
which greatly speeds up [local development](develop.html) .

So set this environment variable as it was shown above:

    export DOCKER_HOST=tcp://vagrant.f8:2375

And don't define any of the other docker env vars like `DOCKER_CERT_PATH` or `DOCKER_TLS_VERIFY`.

Now when in a shell on your host (OS X / Windows) you can type `docker ps` and the output is the same as 
if you type the same command inside the `vagrant ssh` shell inside the vagrant VM.

This then means that your host (OS X / Windows) will use the same docker daemon as the Kubernetes running 
inside the fabric8 vagrant image. Because of this you don't need to use a docker registry; a docker daemon 
is kinda like a local registry anyway - provided you don't try to push or pull the image! 

> **NOTE**  you need to make sure that your Kubernetes JSON uses the imagePullPolicy of `PullIfAbsent` 
> (via the maven property `fabric8.imagePullPolicy`); as any attempt to pull an image just built locally that 
> isn't pushed to a docker registry won't work; since the image isn't public yet. In fabric8 2.2.96 the default in 
> `mvn fabric8:json` is to now omit this value which means it uses the Kubernetes default which is `PullIfAbsent`.

You now should be able to [build and use docker images locally](../developLocally.html)  without pushing or pulling.




















Follow these steps:

* [Download the recent OpenShift release binaries for your platform](https://github.com/openshift/origin/releases/)
* unpack the tarball and put the binaries on your PATH
* Set the following environment variables
  - Unix flavored OSs:
  
          export KUBERNETES_DOMAIN=vagrant.f8
          export DOCKER_HOST=tcp://vagrant.f8:2375

  - Windows:

          set KUBERNETES_DOMAIN=vagrant.f8
          set DOCKER_HOST=tcp://vagrant.f8:2375

 It is probably a good idea to add this into your `~/.profile` (Linux, OS X) or *System* -> *Advance System Settings* -> *Environment Variables* (Windows)
 

* Now login to OpenShift via this command:

```sh
oc login https://172.28.128.4:8443
```

* Enter `admin` and `admin` for user/password

Over time your token may expire and you will need to re-authenticate via:

```sh
oc login
```

Now to see the status of the system:

```sh
oc get pods
```
or you can watch from the command line via one of these commands:

```sh
watch oc get pods
oc get pods --watch
```

Have fun! We [love feedback](http://fabric8.io/community/)


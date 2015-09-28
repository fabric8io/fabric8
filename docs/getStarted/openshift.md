## Getting Started with native OpenShift 

As an alternative to the all-in-one [Vagrant image](vagrant.html), fabric8 can be installed on top of a
native [OpenShift V3](http://www.openshift.org/) Linux installation. 

### Install OpenShift on Linux

The following steps work on a Linux box. If you are on OS X or Windows then check out how to use 
[the Fabric8 Vagrant box](vagrant.html) which contains a full feature OpenShift V3 installation.

#### OpenShift OpenShift installation 

We recommend you check out the [OpenShift Origin Installation documentation](https://docs.openshift.org/latest/getting_started/administrators.html) 
as the default way to install [OpenShift V3](http://www.openshift.org/) before trying the alternative approach. Be sure that you 
install the router and registry and described [below](#openshift-configuration) which has to be done in any case. 

#### Alternative Installation route

Here's an alternative installation approach which could be tried if the guide in the previous section doesn't work 
for you.

* You should have installed [Docker 1.6 or later](https://docs.docker.com/installation/#installation) and have 
  the docker daemon running (see instructions for [CentOs](https://docs.docker.com/installation/centos/), 
  [Fedora](https://docs.docker.com/installation/fedora/) or [Ubuntu](https://docs.docker.com/installation/ubuntulinux/))
* Download and unpack a [release of OpenShift 1.0 or later](https://github.com/openshift/origin/releases/):

```sh
curl -L https://github.com/openshift/origin/releases/download/v1.0.0/openshift-origin-v1.0.0-67617dd-linux-amd64.tar.gz | tar xzv
```

Now setup `$OPENSHIFT_MASTER` to point to the IP address or host name of the OpenShift master:

```sh
export OPENSHIFT_MASTER=https://localhost:8443
```

Then start OpenShift:

```sh
nohup ./openshift start \
        --cors-allowed-origins='.*' \
        --master=$OPENSHIFT_MASTER \
        --volume-dir=/var/lib/openshift/openshift.local.volumes \
        --etcd-dir=/var/lib/openshift/openshift.local.etcd \
        > /var/lib/openshift/openshift.log &
```

When running commands on the OpenShift master type the following to avoid you having to add `--config=....` arguments:

```sh
mkdir -p ~/.kube
ln -s `pwd`/openshift.local.config/master/admin.kubeconfig ~/.kube/config
```

### OpenShift configuration

OpenShift needs some extra installation steps in order to be able to run all the [Fabric8 apps](apps.html). 
Various apps (like [Continuous Delivery](../cdelivery.html) and [MQ](../fabric8MQ.html) requires secrets and service accounts to be setup). 

#### Add roles

The following commands assume you are on the OpenShift master machine :

* Enable the `cluster-admin` role to user `admin`

```
oadm policy add-cluster-role-to-user cluster-admin admin
```

#### Run the gofabric8 installer

[gofabric8](https://github.com/fabric8io/gofabric8) is a useful installer for fabric8. 

[Download a gofabric8 binary](https://github.com/fabric8io/gofabric8/releases) and add it to your `$PATH` 

Now type the following:

```sh
gofabric8 deploy -y
gofabric8 secrets -y
```

At any point you can validate your installation via:

```sh
gofabric8 validate
```

You can also eagerly pull docker images for a Fabric8 template via the `pull` command

```sh
gofabric8 pull cd-pipeline
```


#### Enable the OpenShift router and registry

The [OpenShift Router](https://docs.openshift.org/latest/architecture/core_concepts/routes.html#haproxy-template-router) enables 
external access to services inside a Kubernetes cluster using [HAProxy](http://www.haproxy.org/); e.g. so you can access web apps 
from your browser for apps running inside Kubernetes.

The [OpenShift Registry](https://docs.openshift.org/latest/architecture/infrastructure_components/image_registry.html) is used 
as an internal registry for holding Docker images for Kubernetes.

The detailed instructions for installing these components can be found in the 
[Router installation documentation](https://docs.openshift.org/latest/admin_guide/install/deploy_router.html) and
the [Registry installation documentation](https://docs.openshift.org/latest/admin_guide/install/docker_registry.html). In short it 
boils down to these two commands:

```
oadm router --create --credentials=/var/lib/openshift/openshift.local.config/master/openshift-router.kubeconfig
oadm registry --create --credentials=openshift.local.config/master/openshift-registry.kubeconfig
```

### Next steps

As the next step you can now [setup the OpenShift client](local.html) and install 
the [fabric8 applications](apps.html)

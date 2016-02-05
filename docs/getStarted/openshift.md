## Getting Started with native OpenShift

As an alternative to the all-in-one [Vagrant image](vagrant.html), fabric8 can be installed on top of a
native [OpenShift V3](http://www.openshift.org/) Linux installation.

### Install OpenShift on Linux

The following steps work on a Linux box. If you are on OS X or Windows then check out how to use
[the Fabric8 Vagrant box](vagrant.html) which contains a full feature OpenShift V3 installation.

#### OpenShift Origin installation

We recommend you check out the [OpenShift Origin Installation documentation](https://docs.openshift.org/latest/getting_started/administrators.html)
as the default way to install [OpenShift V3](http://www.openshift.org/) before trying the alternative approach. Be sure that you
install the router and registry as described [below](#openshift-configuration) which has to be done in any case.

#### Alternative Installation route

Here's an alternative installation approach which could be tried if the guide in the previous section doesn't work
for you.

* You should have installed [Docker 1.6 or later](https://docs.docker.com/installation/#installation) and have
  the docker daemon running (see instructions for [CentOs](https://docs.docker.com/installation/centos/),
  [Fedora](https://docs.docker.com/installation/fedora/) or [Ubuntu](https://docs.docker.com/installation/ubuntulinux/))
* Download and unpack a [release of OpenShift 1.0 or later](https://github.com/openshift/origin/releases/):

```sh
curl -L https://github.com/openshift/origin/releases/download/v1.1.1/openshift-origin-server-v1.1.1-e1d9873-linux-64bit.tar.gz | tar xzv
```

Now setup `$OPENSHIFT_MASTER` to point to the IP address or host name of the OpenShift master:

```sh
export OPENSHIFT_MASTER=https://$HOST_IP:8443
```

**Note** Be sure to use the host ip instead of localhost or 127.0.0.1. This ip will be used from inside a container to connect to the openshift api. Therefor localhost would point to the container itself and it won't be able to connect.

Move the extracted folder to be ```/var/lib/openshift/``` and cd into that directory.

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

In order to be able to run `oadm` command add `/var/lib/openshift` to your ``$PATH``:

```sh
export PATH="$PATH:/var/lib/openshift"
```

### OpenShift configuration

OpenShift needs some extra installation steps in order to be able to run all the [Fabric8 apps](apps.html).
Various apps (like [Continuous Delivery](../cdelivery.html) and [MQ](../fabric8MQ.html) requires secrets and service accounts to be setup).

#### Add roles

The following commands assume you are on the OpenShift master machine :

* Enable the `cluster-admin` role to user `admin`

```sh
oadm policy add-cluster-role-to-user cluster-admin admin
```

#### Run the gofabric8 installer

[gofabric8](https://github.com/fabric8io/gofabric8) is a useful installer for fabric8.

[Download a gofabric8 binary](https://github.com/fabric8io/gofabric8/releases), extract it and add it to your `$PATH`

Now type the following:

```sh
gofabric8 deploy -y
gofabric8 secrets -y
```

**Note:** If you install not locally you might have to pass the domain name to the -d option so you can access any app later in the browser. E.g.:

```
gofabric8 deploy -d mydomain.com -y
gofabric8 secrets -y
```

**Note:** If your router lives on another domain/host then your openshift api, you will need to provide the --api-server parameter to point to the api. So if you have your router made accessible by mydomain.com but your openshift master (api) lives at master.mydomain.com run:

```
gofabric8 deploy -d mydomain.com --api-server=master.mydomain.com -y
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

#### Install or upgrade applications using Helm

You can install or upgrade application using the [helm command line tool](http://fabric8.io/guide/helm.html).


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
oadm router --create --service-account=router --credentials=/var/lib/openshift/openshift.local.config/master/openshift-router.kubeconfig
oadm registry --create --credentials=/var/lib/openshift/openshift.local.config/master/openshift-registry.kubeconfig
```

### Next steps

As the next step you can now [setup the OpenShift client](local.html) and install
the [fabric8 applications](apps.html)

## Troubleshooting

If you are having issues logging into the console, ensure you've enabled ```cors-allowed``` as shown above in the ```./openshift start``` command.

Another way to do that is to edit the ```/etc/openshift/master/master-config.yaml``` file [and add an entry like the following](https://github.com/fabric8io/gofabric8/issues/17#issuecomment-149788441):

```
$vi /etc/openshift/master/master-config.yaml
```
then add:
```
corsAllowedOrigins:
  - .*
```

After making a change run:

```
$ restart master
systemctl restart openshift-master
```

Also [make sure your user has a login via web console](https://github.com/fabric8io/fabric8/issues/4866#issue-109652169) e.g. if using HTPasswdPasswordIdentityProvider in OSEv3

```
htpasswd -b /etc/openshift-passwd admin admin
```

### If you ran gofabric8 with the wrong domain

You might have the wrong domain setup for the fabric8 ServiceAccount. You should be able to see this via

```
oc get oauthclient fabric8
```

If its got the wrong domain for the redirect URIs, just delete it and re-run gofabric8.

```
oc delete oauthclient fabric8
gofabric8 deploy -y -d my-new-domain.com
gofabric8 secrets -y
oc get oauthclient fabric8
```

Though there's a pending issue to do this automatically whenever you run gofabric8 deploy

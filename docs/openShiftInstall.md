## Install OpenShift

We recommend you check out the [OpenShift Origin Installation documentation](http://docs.openshift.org/latest/getting_started/dev_get_started/installation.html) as the default way to install [OpenShift V3](http://www.openshift.org/).

The following steps work on a linux box.

If you are on OS X or Windows then check out how to use [the fabric8 vagrant box](getStartedVagrant.html).

### Install steps

* you should have installed [Docker 1.6 or later](https://docs.docker.com/installation/#installation) and have the docker daemon running (see instructions for [CentOs](https://docs.docker.com/installation/centos/), [Fedora](https://docs.docker.com/installation/fedora/) or [Ubuntu](https://docs.docker.com/installation/ubuntulinux/))
* download and unpack a [release of OpenShift 1.0 or later](https://github.com/openshift/origin/releases/):

```
curl -L https://github.com/openshift/origin/releases/download/v1.0.0/openshift-origin-v1.0.0-67617dd-linux-amd64.tar.gz | tar xzv
```


Now setup `$OPENSHIFT_MASTER` to point to the IP address or host name of the OpenShift master:

```
export OPENSHIFT_MASTER=https://localhost:8443
```
Then start OpenShift:
```
nohup ./openshift start \
        --cors-allowed-origins='.*' \
        --master=$OPENSHIFT_MASTER \
        --volume-dir=/var/lib/openshift/openshift.local.volumes \
        --etcd-dir=/var/lib/openshift/openshift.local.etcd \
        > /var/lib/openshift/openshift.log &
```

When running commands on the OpenShift master type the following to avoid you having to add --config=.... arguments:

```
mkdir -p ~/.kube
ln -s `pwd`/openshift.local.config/master/admin.kubeconfig ~/.kube/config
```

Now enable the cluster-admin role:

```
./osadm policy add-cluster-role-to-user cluster-admin admin
```

Now you can run the router (haproxy to expose services publically) and registry (docker registry):

```
./osadm router --create --credentials=openshift.local.config/master/openshift-router.kubeconfig
./osadm registry --create --credentials=openshift.local.config/master/openshift-registry.kubeconfig
```

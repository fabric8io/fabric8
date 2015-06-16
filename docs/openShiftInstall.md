## Install OpenShift

We recommend you check out the [OpenShift Origin Installation documentation](http://docs.openshift.org/latest/getting_started/dev_get_started/installation.html) as the default way to install [OpenShift V3](http://www.openshift.org/).

The following steps work on a linux box.

If you are on OS X or Windows then check out how to use [the fabric8 vagrant box](openShiftWithFabric8Vagrant.html).

### Install steps

* you should have installed [Docker 1.6 or later](https://docs.docker.com/installation/#installation) and have the docker daemon running (see instructions for [CentOs](https://docs.docker.com/installation/centos/), [Fedora](https://docs.docker.com/installation/fedora/) or [Ubuntu](https://docs.docker.com/installation/ubuntulinux/))
* download and unpack a [release of OpenShift 0.5.1 or later](https://github.com/openshift/origin/releases/):

```
curl -L https://github.com/openshift/origin/releases/download/v0.5.1/openshift-origin-v0.5.1-ce1e6c4-linux-amd64.tar.gz | tar xzv
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
mkdir -p ~/.config/openshift
ln -s `pwd`/openshift.local.config/master/admin.kubeconfig ~/.config/openshift/config
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

### Configure OpenShift

OpenShift needs some extra installation steps in order to use the fabric8 applications as described in
"[Install Fabric8 on OpenShift](fabric8OnOpenShift.html)":

### Add roles

The following commands assume you are on the OpenShift master machine in a folder containing the `openshift.local.config` directory:

* Enable the `cluster-admin` role to user `admin`

```
osadm policy add-cluster-role-to-user cluster-admin admin --config=openshift.local.config/master/admin.kubeconfig
```

### Add secrets

* each namespace you wish to install fabric8 into typically requires the `openshift-cert-secrets`


You'll also need to login and switch to the correct project (namespace):

```
osc login
osc project default
```

Then run this command:

```
cat <<EOF | osc create -f -
---
	apiVersion: "v1"
	kind: "Secret"
	metadata:
		name: "openshift-cert-secrets"
	data:
		root-cert: "$(base64 -w 0 openshift.local.config/master/ca.crt)"
		admin-cert: "$(base64 -w 0 openshift.local.config/master/admin.crt)"
		admin-key: "$(base64 -w 0 openshift.local.config/master/admin.key)"
EOF
```

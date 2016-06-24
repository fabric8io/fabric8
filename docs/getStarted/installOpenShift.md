## Install OpenShift

If you don't have an OpenShift cluster yet then please check out the [various options](https://www.openshift.com/) or check out the [OpenShift Origin getting started guide](https://docs.openshift.org/latest/getting_started/administrators.html) or try [these OpenShift setup tips](installOpenShift.html).

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

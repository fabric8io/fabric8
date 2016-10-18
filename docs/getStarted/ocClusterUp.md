## Getting Started with `oc cluster up`

The [oc cluster up](https://github.com/openshift/origin/blob/master/docs/cluster_up_down.md#overview) command starts a local OpenShift all-in-one cluster with a configured registry, router, image streams, and default templates. By default, the command requires a working Docker connection. However, if running in an environment with Docker Machine installed, it can create a Docker machine for you.

### Start the cluster

Follow the [getting started guide](https://github.com/openshift/origin/blob/master/docs/cluster_up_down.md#getting-started) to start the cluster

### Run the gofabric8 installer

* [Download a gofabric8 binary for your platform](https://github.com/fabric8io/gofabric8/releases), extract it and add it to your `$PATH`

Now type the following:

```sh
gofabric8 deploy -y --domain=$(docker-machine ip openshift).xip.io --api-server=$(docker-machine ip openshift)
```

At any point you can validate your installation via:

```sh
gofabric8 validate
```

### Extra setup

As this is using the OpenShift all-in-one cluster we have to update the origin master config so that the fabric8 console can work.  These steps are a little fiddly so hopefully we can find a better way to automate this.

get the fabric8 console hostname

```sh
oc get -o jsonpath="{.spec.host}" route fabric8
```

copy the output, should be something like:

```
fabric8.192.168.99.101.xip.io
```

SSH to the docker machine that's running openshift and edit the master config

```sh
docker-machine ssh openshift
sudo vi /var/lib/origin/openshift.local.config/master/master-config.yaml
```

add the hostname from above to the list of allowed CORS hostnames, e.g.

```
corsAllowedOrigins:
- 127.0.0.1
- 192.168.99.101:8443
- localhost
- fabric8.192.168.99.101.xip.io
```

now restart the origin master from inside the docker-machine:

```sh
docker restart origin
```

oc cluster up suggests to use the `developer:developer` username and password to login. If you use this account then you'll need to give it permissions to see the default team that gets created. Easiest way to do this is run:

```sh
oc adm policy add-cluster-role-to-user cluster-admin developer
```

### Access the Fabric8 Developer Console

To open the [Fabric8 Developer Console](../console.html) then type the following:


```sh
open http://$(oc get -o jsonpath="{.spec.host}" route fabric8)
```

#### Configuring Docker

To use docker on your host communicating with the docker daemon inside your OpenShift cluster type:

```sh
eval $(docker-machine env openshift)
```

## Troubleshooting

Check out the [troubleshooting guide](troubleshooting.html) for more help.

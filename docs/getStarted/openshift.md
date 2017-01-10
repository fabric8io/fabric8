## Getting Started with OpenShift

As an alternative to the all-in-one [Vagrant image](vagrant.html), fabric8 can be installed on top of a native [OpenShift V3](http://www.openshift.org/) Linux installation.

If you don't have an OpenShift cluster yet then please check out the [various options](https://www.openshift.com/), check out the [OpenShift Origin getting started guide](https://docs.openshift.org/latest/getting_started/administrators.html) or try [these OpenShift setup tips](installOpenShift.html)

### OpenShift configuration

OpenShift needs some extra installation steps in order to be able to run all the [Fabric8 apps](apps.html).

#### Add roles

The following commands assume you are on the OpenShift master machine :

* Enable the `cluster-admin` role to user `admin`

```sh
oc adm policy add-cluster-role-to-user cluster-admin admin
```

* Enable the `cluster-reader` role to Service Accounts

```sh
oc adm policy add-cluster-role-to-group cluster-reader system:serviceaccounts
```

### Run the gofabric8 installer

[gofabric8](https://github.com/fabric8io/gofabric8) is a useful installer for fabric8.

[Download a gofabric8 binary](https://github.com/fabric8io/gofabric8/releases), extract it and add it to your `$PATH`

Now type the following:

```sh
gofabric8 deploy
```

**Note:** If you install not locally you might have to pass the domain name to the -d option so you can access any app later in the browser. E.g.:

```
gofabric8 deploy -d mydomain.com
```

**Note:** If your router lives on another domain/host then your openshift api, you will need to provide the --api-server parameter to point to the api. So if you have your router made accessible by mydomain.com but your openshift master (api) lives at master.mydomain.com run:

```
gofabric8 deploy -d mydomain.com --api-server=master.mydomain.com
```

By default the full [Fabric8 Microservices Platform with CI / CD support](../cdelivery.html) is installed.  If you want just install the fabric8 console then you can add the `--app` command line argument and leaving the value blank

```
gofabric8 deploy -y -d mydomain.com --app=
```


#### Persistence

New releases of fabric8 now have persistence enabled for some apps (like gogs, nexus, jenkins), so please see this guide on [creating the necessary persistent volumes or opting out of persistence](persistence.html)


#### Validating your install

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

Check out the [troubleshooting guide](troubleshooting.html) for more help.

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

If its got the wrong domain for the redirect URIs, make sure you have the [latest gofabric8 version](https://github.com/fabric8io/gofabric8/releases) on your `$PATH` then just re-run gofabric8
```
gofabric8 deploy -y -d my-new-domain.com
gofabric8 secrets -y
oc get oauthclient fabric8
```

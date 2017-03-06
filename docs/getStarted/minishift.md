## Getting Started with MiniShift

### Warning: please use minikube!

**NOTE** the new [1.x version of MiniShift](https://github.com/minishift/minishift) does not yet work with fabric8. Due to the 1.x changes of MiniShift suport for Fabric8 and MiniShift is in a state of flux right now and it looks like not getting fixed any time soon.

Until 1.x of minishift is working with fabric8 **we highly recommend** you just stick with [minikube](minikube.html) instead for now until it gets resolved!

If you still want to try with minishift 1.x here are some tips to try work around the regressions in minishift:

* start minishift manually (don't use `gofabric8 start`)
* install oc by hand. e.g. via:

    brew install openshift-cli

* manually enable CORS due to [this issue](https://github.com/minishift/minishift/issues/257) 


    minishift openshift config set --patch '{"corsAllowedOrigins": [".*"]}'

* only login with `system:admin` as the `developer` account is pretty broken; [unable to create projects](https://github.com/minishift/minishift/issues/476) via `oc apply` or the REST API and [unable to create Persistent Volumes](https://github.com/minishift/minishift/issues/458)

     oc login -u system:admin
          
* you might wanna try figure out how to [reconfigure minishift to use `nip.io` instead of `xip.io`](https://github.com/minishift/minishift/issues/501) as the latter is a little flaky

### Start the cluster with minishift start

* [Download a MiniShift binary for your platform](https://github.com/jimmidyson/minishift/releases), extract it and add it to your `$PATH`
* type the following command to create a new OpenShift cluster:

```sh
minishift start --memory=6000
```

Then follow the on screen prompts.

You should now be able to connect to the cluster via the `kubectl` command line tool from Kubernetes or the `oc` command from [openshift origin client tools](https://github.com/openshift/origin/releases) for your platform.

```sh
oc get pods
```

### Run the gofabric8 installer

* [Download a gofabric8 binary for your platform](https://github.com/fabric8io/gofabric8/releases), extract it and add it to your `$PATH`

Now type the following:

```sh
gofabric8 deploy -y
```

At any point you can validate your installation via:

```sh
gofabric8 validate
```


### Access the Fabric8 Developer Console

To open the [Fabric8 Developer Console](../console.html) then type the following:

```sh
minishift service fabric8
```

Then a browse window will open for the console.

To see the URL so you can open it in another browser you can type:

```sh
minishift service fabric8 --url
```

You can use the same command to open other consoles too like gogs, Jenkins or Nexus

```sh
minishift service gogs
minishift service jenkins
minishift service nexus
```

Though from the [Fabric8 Developer Console](../console.html) you can easily switch between all your development tools using the tool drop down menu at the top right of the screen:

![clicking on the tools drop down](../images/console-tools.png)

#### Configuring Docker

To use docker on your host communicating with the docker daemon inside your MiniShift cluster type:

```sh
eval $(minishift docker-env)
```

#### Enable the OpenShift Router

If you wish to run the [OpenShift Router](https://docs.openshift.org/latest/architecture/core_concepts/routes.html#haproxy-template-router) to generate nicer URLs to access services inside your cluster instead of the default `nodePort` approach then try:

```sh
oc adm router --create --service-account=router --expose-metrics --subdomain="$(minishift ip).xip.io"
```

Note that sometimes this can fail to deploy if it takes too long to pull the images on your machine. So you could try pulling the docker image first:

```sh
eval $(minishift docker-env)
docker pull openshift/origin-haproxy-router:`oc version | awk '{ print $2; exit }'`
```

## Troubleshooting

Check out the [troubleshooting guide](troubleshooting.html) for more help.

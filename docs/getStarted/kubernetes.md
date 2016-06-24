## Getting started with Kubernetes

First you wiil need a Kubernetes cluster to play with. If you don't yet have a Kubernetes cluster you could try [these instructions to install a Kuberentes cluster](http://kubernetes.io/docs/getting-started-guides/binary_release/#download-kubernetes-and-automatically-set-up-a-default-cluster) or try [GKE](gke.html).

### Checking your connection

Once you have your kubernetes cluster then check you can connect to it via:

```
kubectl get pods
```

You may also want to [setup bash completion for kubectl](https://blog.fabric8.io/enable-bash-completion-for-kubernetes-with-kubectl-506bc89fe79e#.wswsvb7y7) which can make working with `kubectl` much more fun ;).

### Run the gofabric8 installer

The easiest way to install fabric8 on Kubernetes or OpenShift is the [gofabric8 command line tool](https://github.com/fabric8io/gofabric8).

[Download the latest gofabric8 binary](https://github.com/fabric8io/gofabric8/releases), extract it and add it to your `$PATH`

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

**Note:** If you wish to use a different domain name to your kubernetes API server, you will need to provide the --api-server parameter to point to the api. e.g.:

```
gofabric8 deploy -d mydomain.com --api-server=master.mydomain.com -y
gofabric8 secrets -y
```

If you wish to install the full [Fabric8 Microservices Platform with CI / CD support](../cdelivery.html) or any other app you can add the `--app` command line argument:

```
gofabric8 deploy -y -d mydomain.com --app=cd-pipeline
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


### Next steps

You can now install more [fabric8 applications](apps.html).

## Troubleshooting

Check out the [troubleshooting guide](troubleshooting.html) for more help.


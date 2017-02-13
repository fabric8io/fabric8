# Getting Started with Kubernetes on AWS

It has been very easy to get up and running with AWS using Ansible but now there's an even easier way using [stackpoint.io](https://stackpoint.io/). Before beginning you'll probably want to familiarise yourself with the AWS pricing model https://aws.amazon.com/ec2/pricing/

## Creating a container cluster

Now you are ready to create a cluster on AWS.  To start with we recommend creating a cluster of two or three instances which can be used to familiarise yourself with the architecture and components without incurring too much cost.  You can easily build up the cluster later.   

In order to run the fabric8 CD pipelines features you'll probably want your nodes to have 8-16GiB so that we can schedule multiple Jenkins Agents at the same time.

We now have two options to create you Kubernetes cluster on AWS:

### 1. stackpoint.io

Our recommended approach is to use [stackpoint.io](https://stackpoint.io/) as the easiest way to spin up a Kubernetes cluster on AWS.

Follow the simple stackpoint.io guide until you have a running cluster then [Install the fabric8 microservices platform default applications](#install-the-fabric8-microservices-platform)

or follow this [short video](https://www.youtube.com/watch?v=lNRpGJTSMKA)

<div class="row">
  <p class="text-center">
      <iframe src="https://www.youtube.com/embed/lNRpGJTSMKA" width="750" height="425" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>
  </p>
</div>

### 2. Kubernetes Ansible scripts

To start you will need to sign up for an account and obtain your AWS Access Key ID and Secret Access Key as well as get the AWS CLI client.  Follow the prerequisits in the Kubernetes docs.

- [prerequisites](http://kubernetes.io/docs/getting-started-guides/aws/#prerequisites)

```
export KUBERNETES_PROVIDER=aws # use the AWS specific scripts
export KUBE_AWS_ZONE=eu-west-1c # choose your region, the default is us-west-2
export NUM_NODES=3 # choose the number of nodes you want  
export MASTER_SIZE=m3.large
export NODE_SIZE=m3.xlarge
export KUBE_ENABLE_INSECURE_REGISTRY=true # required to set the insecure registry flag on each node so we can push images to the cluster docker registry
```

Now lets run the Kubernetes install script
```
wget -q -O - https://get.k8s.io | bash
```

Check your nodes are running

```
kubectl get nodes
```

### Persistence

A number of fabric8 applications require persistent storage so that we don't loose data when pods are restarted.  Examples are Gogs, Jenkins, Nexus and ElasticSearch.

We use Kubernetes dynamic `PersistentVolume` provisioning to automatically create PVs when a `PersistentVolumeClaim` needs one.

Create a StorageClass that points to a persistence implementation for your cloud ensuring your `StorageClass` has a name `name: standard` see examples below.  For a full explaination see [persistence](getStarted/persistence.md)

Run:

    cat <<EOF | kubectl create -f -
    kind: StorageClass
    apiVersion: storage.k8s.io/v1beta1
    metadata:
      name: standard
    provisioner: kubernetes.io/aws-ebs
    parameters:
      type: gp2
    EOF

## Install the fabric8 microservices platform

Next we want to deploy the fabric8 microservices platform default components on top of Kubernetes, get the latest `gofabric8` binary from  [gofabric8](https://github.com/fabric8io/gofabric8/releases) and run

```
gofabric8 deploy
```
gofabric8 will use the local credentials on your remote machine from `~/.kube/config` after the authentication script above

It may make a few minutes to download a number of docker images but once done your broswer will open to the fabric8 console

### Ingress

fabric8's preferred approach to accessing applications and services running on Kubernetes is to use the [NGINX Ingress Controller](https://github.com/nginxinc/kubernetes-ingress/tree/master/nginx-controller).  By default we deploy an Ingress NGINX controller on the first scheduble node we can find using the node label `externalIP=true`.  gofabric8 sets this so if you want your ingress controller on another node which has an externally accessible IP (used to configure your wildcard DNS) then move the label

```
kubectl label node $EXISTINGNODE  externalIP-
kubectl label node $NEWINGNODE  externalIP=true
```
By default [exposecontroller](https://github.com/fabric8io/exposecontroller) is also deployed which will watch for services across you namespaces and automatically create Ingress rules so you can access you applications.  We use `[service-name].[namespace].[domain]`

e.g.
```
http://fabric8.default.fabric8.io
```

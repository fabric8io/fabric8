## Getting Started with Kubernetes on Google Container Engine

It is very easy to get up and running with Google Container Engine and the docs are extremely well structured.  Before you start you should read the [overview](https://cloud.google.com/container-engine/docs/#overview) and familiarise yourself with the [pricing](https://cloud.google.com/container-engine/docs/#pricing) information.

### Before you begin
To start you will need to sign up for an account, enable billing and install on your local machine the `gcloud` and `kubectl` client binaries.  All this can be done by following the [Google before you begin guide](https://cloud.google.com/container-engine/docs/before-you-begin).

### Creating a container cluster

Now you are ready to create a cluster on Google Container Engine.  To start with we recommend creating a cluster of two or three instances which can be used to familiarise yourself with the architecture and components without incurring too much cost.  You can easily build up the cluster later.

Go to [Google Container Engine website](https://cloud.google.com/container-engine/), from here you can click to be taken to your [Console](https://console.cloud.google.com/kubernetes/).

![GKE create cluster screenshot](../images/gkeCreateClusters.png)

Now that you've created your cluster you should be able to use `kubectl` as described above to remotely connect and interact with your new Kubernetes cluster. For example to authenticate, set the following ENV VARS in a terminal taken from the Google Container Engine console.

![GKE create cluster screenshot](../images/gkeCredentials.png)

```
KUBERNETES_USERNAME=
KUBERNETES_PASSWORD=
KUBERNETES_SERVER=
```

Once the above ENV VARS are set you can authenticate against your new cluster from your local machine using

```
kubectl config set-credentials demo --username=$KUBERNETES_USERNAME --password=$KUBERNETES_PASSWORD
kubectl config set-cluster demo --insecure-skip-tls-verify=true --server=https://$KUBERNETES_SERVER
kubectl config set-context demo --user=demo --namespace=default --cluster=demo
kubectl config use-context demo
```

Check your nodes are running

```
kubectl get nodes
```

We will be deploying an NGINX Ingress controller so that we can access our applications running on Kubernetes.  In order to access the contoller we will need to open up two firewalls on Google Cloud:

```
gcloud compute firewall-rules create fabric8-http --allow tcp:80,icmp
gcloud compute firewall-rules create fabric8-https --allow tcp:443,icmp
```

### Install the fabric8 microservices platform default applications

__WARNING__ fabric8 has recently added support for persistent volumes but we don't have this available on GKE yet.  Pods that run on GKE with the OOTB configuration will loose data if a pod is restarted.

Next we want to deploy the fabric8 microservices platform components on top of Kubernetes, get the latest `gofabric8` binary from  [gofabric8](https://github.com/fabric8io/gofabric8/releases) and run

```
gofabric8 deploy
```
gofabric8 will use the local credentials on your remote machine from `~/.kube/config` after the authentication script above

It may make a few minutes to download a number of docker images but once the console is available the your browser should open and be taken to the dashboard.

### Using the console

Here is a [video showing you what you can do with the console on Google Container Engine](https://vimeo.com/173353537)


<div class="row">
  <p class="text-center">
      <iframe src="https://player.vimeo.com/video/172948055" width="1000" height="562" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>
  </p>
</div>

For more details check the [console documentation](console.html)

### Container Registry

Your Google Container Engine project has a [container registry](https://cloud.google.com/tools/container-registry/) that you can use to push images to and reference in you kubernetes configurations.  This is useful if you want to avoid pulling images from dockerhub and also when developing custom images.

For example if your Google Container Engine project ID is `fabric8-984`, to Docker build, tag and push to your Google projects container registry

```
docker build --rm -t gcr.io/fabric8-984/fabric8-console .
gcloud docker push gcr.io/fabric8-984/fabric8-console
```

### Google Container Engine Quotas

It's easy to exceed the default limits provided by Google Container Engine when starting out.  Navigating to the Google Container Engine Admin dashboard allows you to see how you are doing with your quotas.  For example it was easy to exceed the basic number of forwarding rules, static IP's and firewalls.  Here are a few `gcloud` commands that can help find and optionally delete ALL resources

```
gcloud compute addresses list
gcloud compute target-pools list
gcloud compute firewall-rules list
gcloud compute forwarding-rules list
```
then you can delete any resources using the UID, e.g.

```
gcloud compute addresses delete ae1a489594dc311e6876942010af0009
```

or to delete ALL resources
```
gcloud compute addresses delete $(gcloud compute addresses list | cut -f 1 -d ' ')
gcloud compute target-pools delete $(gcloud compute target-pools list | cut -f 1 -d ' ')
gcloud compute firewall-rules delete $(gcloud compute firewall-rules list | cut -f 1 -d ' ')
gcloud compute forwarding-rules delete $(gcloud compute forwarding-rules list | cut -f 1 -d ' ')
```

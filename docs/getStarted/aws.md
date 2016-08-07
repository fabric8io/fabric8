## Getting Started with Kubernetes on AWS

It is very easy to get up and running with AWS and the docs are extremely well structured.  Before you start you should read the [overview](https://cloud.google.com/container-engine/docs/#overview) and familiarise yourself with the [pricing](https://cloud.google.com/container-engine/docs/#pricing) information.

### Before you begin
To start you will need to sign up for an account and obtain your AWS Access Key ID and Secret Access Key as well as get the AWS CLI client.  Follow the prerequisits in the Kubernetes docs.

- [prerequisites](http://kubernetes.io/docs/getting-started-guides/aws/#prerequisites)

### Creating a container cluster

Now you are ready to create a cluster on AWS.  To start with we recommend creating a cluster of two or three instances which can be used to familiarise yourself with the architecture and components without incurring too much cost.  You can easily build up the cluster later.  We'll also set a few more environment variables that the Kubernetes install script will use.  

```
export KUBERNETES_PROVIDER=aws # use the AWS specific scripts
export KUBE_AWS_ZONE=eu-west-1c # choose your region, the default is us-west-2
export NUM_NODES=4 # choose the number of nodes you want  
export MASTER_SIZE=m3.medium
export NODE_SIZE=m3.medium
export KUBE_ENABLE_INSECURE_REGISTRY=true # required to set the insecure registry flag on each node so we can push images to the cluster docker registry
wget -q -O - https://get.k8s.io | bash
```

Check your nodes are running

```
kubectl get nodes
```

### Install the fabric8 microservices platform default applications

Next we want to deploy the fabric8 microservices platform components on top of Kubernetes, get the latest `gofabric8` binary from  [gofabric8](https://github.com/fabric8io/gofabric8/releases) and run

```
gofabric8 deploy
```
gofabric8 will use the local credentials on your remote machine from `~/.kube/config` after the authentication script above

It may make a few minutes to download a number of docker images but to track progress you can watch progress using
```
kubectl get pod -w
```
As soon as the fabric8-xxxx pod is running you can open a URL to the fabric8 console using the external service address.  Here's a quick command to get the console URL and open it.
```
open http://$(kubectl get svc -o=wide| grep -w 'fabric8 ' |  awk '{print $3}')
```

### Load Balancer

The AWS Load Balancer can take a minute or two to create an external IP that can be used to access your services.  If you cannot access services using the external loadbalancer you might need to leave it a minute or two before retrying.  We are moving to using the NGINX ingress controller instead of exteral loadbalancer IPs so this is a short term issue. 

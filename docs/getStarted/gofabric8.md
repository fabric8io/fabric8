# gofabric8

fabric8 uses a CLI that makes installing fabric8 locally or on remote Kubernetes based clusters very easy.

gofabric8 also has lots of handy commands that makes it easier to work with fabric8 and OpenShift / Kubernetes

## Download gofabric8

Download the latest gofabric8 release from [GitHub](https://github.com/fabric8io/gofabric8/releases/latest/) or run this script:
```
curl -sS https://get.fabric8.io/download.txt | bash
```
add the binary to your $PATH so you can execute it
```
echo 'export PATH=$PATH:~/.fabric8/bin' >> ~/.bashrc
source ~/.bashrc
```
or for __oh-my-zsh__
```
echo 'export PATH=$PATH:~/.fabric8/bin' >> ~/.zshrc
source ~/.zshrc
```

### Linux

If you are on linux then first you must [install the KVM driver](https://github.com/minishift/minishift/blob/master/docs/source/getting-started/setting-up-driver-plugin.adoc#kvm-driver-install).

## Setup GitHub client ID and secret

We now have GitHub integration letting you browse + create new repositories, edit projects and setup automated CI / CD jobs with webhooks on github.

This requires an [OAuth application to be setup on your github account](https://developer.github.com/apps/building-integrations/setting-up-and-registering-oauth-apps/registering-oauth-apps/) for fabric8 and you need to obtain the client ID and secret for the OAuth application.

We need to set up an oauth client in GitHub so we can reuse their authentication, initially with a dummy redirect URI until gofabric8 gives us the correct one once the external keycloak URL is avaialble.

So please follow the steps below using the a redirect URL such as:
```
http://keycloak-fabric8.{minishift ipv4 value}.nip.io/auth/realms/fabric8/broker/github/endpoint
```

and `https://fabric8.io` as the sample homepage URL:


![Register OAuth App](./images/register-oauth.png)


Once you have created the OAuth application for fabric8 in your github settings and found your client ID and secret then set the env vars below replacing the values:

```
export GITHUB_OAUTH_CLIENT_ID=123
export GITHUB_OAUTH_CLIENT_SECRET=123abc
```

### Quickstart

If you're starting from scratch and don't have minishift / minikube installed or the client binaries used to interact with them or drivers even, then simply run:

__Minikube__
```
gofabric8 start --package=system  --namespace fabric8
```
__Minishift__
```
gofabric8 start --minishift --package=system  --namespace fabric8
```

Which should download all you need, start a kubernetes cluster and install fabric8 on top. Log into fabirc8 as "developer/developer"

Otherwise please read on for more detail on the different options.


### Deploying to Minikube

We require a recent version of minikube. If you are upgrading from an old installation of minikube we recommend you run something like this for minikube:

```
minikube delete
sudo rm -rf ~/.minikube
```

Then [download the latest minikube release](https://github.com/kubernetes/minikube/releases) and put it into your `PATH`.

Then to start minikube and install fabric8 type:

```
minikube start --vm-driver=xhyve --cpus=5 --disk-size=50g --memory=8000
minikube addons enable ingress
gofabric8 deploy --package system -n fabric8
```

If the `minikube start` command fails please see the [minikube instructions](https://github.com/kubernetes/minikube#quickstart)


### Deploying to Minishift

If you are on linux then first you must [install the KVM driver](https://github.com/minishift/minishift/blob/master/docs/source/getting-started/setting-up-driver-plugin.adoc#kvm-driver-install).

We require a [recent release of minishift](https://github.com/minishift/minishift/releases). If you are upgrading from an old installation of minishift we recommend you run something like this for minikube:

```
minishift delete
sudo rm -rf ~/.minishift
minishift update
```

* Make sure you have a recent (3.5 of openshift or 1.5 of origin later) distribution of the `oc` binary on your `$PATH`
```
oc version
```

* If you have an old version or its not found please [download a distribution of the openshift-client-tools for your operating system](https://github.com/openshift/origin/releases/latest/) and copy the `oc` binary onto your `$PATH`

* [download the minishift distribution for your platform](https://github.com/minishift/minishift/releases) extract it and place the `minishift` binary on your `$PATH` somewhere
* start up minishift via something like this (on OS X):

```
minishift start --vm-driver=xhyve --memory=7000 --cpus=4 --disk-size=50g
```
or on any other operating system (feel free to add the `--vm-driver` parameter of your choosing):

```
minishift start --memory=7000 --cpus=4 --disk-size=50g
```

If the `minishift start` command fails please see the [minishift instructions](https://docs.openshift.org/latest/minishift/getting-started/index.html)

* now use gofabric8

```
gofabric8 deploy --package system -n fabric8
```


### Installing on remote public Kubernetes clusters

Get a connection to your cluster so that the following command works:
```
kubectl get nodes
```
Now deploy fabric8:
```
gofabric8 deploy --package system --http=true --legacy=false -n fabric8
```
By default we will use the magic domain `nip.io` when generating ingress rules when deployed as above.  If you provide your own domain string that you want fabric8 to use when generating ingress rules then we also deploy kube-lego which will automatically generate + refresh signed certificates for you.

To use this option and have https signed certs generated automatically for your domain run this instead:
```
export TLS_ACME_EMAIL=email.address@for.certbot.com
gofabric8 deploy --package system --domain example.domain.fabric8.io --legacy=false -n fabric8
```

Though please be aware that if you omit the `--http=true` CLI flag then HTTPS will be used with cert generation via kube-lego; which works great but you will get rate limited if you try to reinstall fabric8 a few times into the same domain which will [result in this error](https://github.com/fabric8-services/fabric8-auth/issues/105). 

Sticking with HTTP instead of HTTPS is our best option [until kube-lego supports wildcard DNS](https://letsencrypt.org/2017/07/06/wildcard-certificates-coming-jan-2018.html) or can reuse the same Certs across reinstalls of fabric8. 

### Installing on remote public OpenShift clusters

Installing on a remote public OpenShift clusters will be the same process as the Kubernetes install. You make sure you are logged in into the remote OpenShift cluster first before deploying :

```
oc login
```

And deploy as per the instructions for Kubernetes remote install.

If you have deployed with the [oc cluster up](https://github.com/openshift/origin/blob/master/docs/cluster_up_down.md) command you may run into some selinux issues and you have to run this command to change the context of the local volumes to allow to write to it :

```
chcon -Rt svirt_sandbox_file_t /var/lib/origin/openshift.local.volumes/
```

### Local development

If you are developing locally and want to deploy custom version of YAML then you can clone this repo and run:

```
mvn clean install  -DskipTests=true
```
MiniKube / Kubernetes
```
gofabric8 deploy --namespace fabric8 --legacy=false -y --package=packages/fabric8-system/target/classes/META-INF/fabric8/k8s-template.yml
```
MiniShift / OpenShift
```
gofabric8 deploy --namespace fabric8 --legacy=false -y --package=packages/fabric8-system/target/classes/META-INF/fabric8/openshift.yml
```

### Accept the insecure URLs in your browser - remote OpenShift clusters ONLY

Currently there are 4 different URLS that Chrome will barf on and you'll have to explcitily click on the `ADVANCED` button then click on the URL to tell your browser its fine to trust the URLs before you can open and use the new fabric8 console

The above script should list the 4 URLs you need to open separately and approve.

We hope to figure out a nicer alternative to this issue! The problem is things like lenscript only work for public hosted URLs; whereas running locally on MiniShift we're local but use `nip.io` to provide a global URL to your local machine (to simplify having to do DNS magic on your laptop). If you fancy trying to help fix this [please check out this MiniShift issue](https://github.com/minishift/minishift/issues/1031)

### Troubleshooting

* __Pods fail to start__ - init container issues: check the init container logs
```
oc logs foo -c init-container-name
```
* __Networking issues__ - cannot connect to github for example: see https://docs.openshift.com/container-platform/3.6/admin_guide/sdn_troubleshooting.html

### FAQ

### I need to manually create the OAuthCLient

Creating OAuthClients requires cluster permissions so not everyone has this.  If you need to manually create or request the OAuthclient be created you can use this (remember to replace `$YOUR_DOMAIN` with your domain)

```
oc login -u system:admin

cat <<EOF | oc create -f -
kind: OAuthClient
apiVersion: v1
metadata:
  name: fabric8-online-platform
secret: fabric8
redirectURIs:
- "http://keycloak-fabric8.$YOUR_DOMAIN/auth/realms/fabric8/broker/openshift-v3/endpoint"
grantMethod: prompt
EOF
```
## Installing Fabric8 inside the CDK

First you will need to:

* Download and install [VirtualBox](https://www.virtualbox.org/wiki/Downloads)
* Download and install [Vagrant](http://www.vagrantup.com/downloads.html)

Next clone the [CDK vagrant repository](https://github.com/redhat-developer-tooling/openshift-vagrant) and follow the [prerequisites](https://github.com/redhat-developer-tooling/openshift-vagrant#prerequisites)

```
git clone https://github.com/redhat-developer-tooling/openshift-vagrant.git
cd openshift-vagrant

cd cdk-v2
export OPENSHIFT_VAGRANT_USE_OSE_3_2=true
export SUB_USERNAME=...  
export SUB_PASSWORD=...  

export VM_CPU=4
export VM_MEMORY=6120

vagrant plugin install vagrant-registration
vagrant plugin install vagrant-service-manager
vagrant plugin install vagrant-sshfs

vagrant up
```

Once that is completed you should have the OpenShift console at https://10.1.2.2:8443/console/

Now download and install [gofabric8](https://github.com/fabric8io/gofabric8/releases) and ensure its on your `$PATH`.

So ssh to your vagrant machine, download the latest release using curl, decompress the file and move gofabric8 to the /usr/local/bin folder.

```
curl -sSL https://github.com/fabric8io/gofabric8/releases/download/LATEST_VERSION/gofabric8-LATEST_VERSION-linux-amd64.tar.gz | tar -xzv
sudo cp gofabric8 /usr/local/bin
```

You will probably want to login so you can use the `oc` command line tool from OpenShift:

```
oc login 10.1.2.2:8443 -u=admin -p=admin
```

Now install the [fabric8 developer console](../console.html):
```
gofabric8 deploy -y --domain=openshift.10.1.2.2.xip.io
gofabric8 secrets -y
```

If you wish to install the full [Fabric8 Microservices Platform with CI / CD support](../cdelivery.html) then try this command instead
```
gofabric8 deploy -y --domain=openshift.10.1.2.2.xip.io --app=cd-pipeline
gofabric8 secrets -y
```

It will take a few moments to startup and download the [Fabric8 Developer Console](../console.html), you should see the pod startup in the OpenShift console or via the commmand:
```
oc get pods -w
```

Now that the fabric8 console is up and running you should be able to access it at: http://fabric8.openshift.10.1.2.2.xip.io/

From there you should be able to start running apps and having fun! :) 


### Troubleshooting

Check out the [troubleshooting guide](troubleshooting.html) for more help.

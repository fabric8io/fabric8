## Fabric8 Vagrant Box

We maintain a [Vagrant](http://www.vagrantup.com/downloads.html) image if you want to run OpenShift easily with Fabric8 on non-Linux platforms.

If you are on a linux box then you might want to [use the Linux instructions](setupOpenShift.html#if-you-are-on-a-linux).

### Create the vagrant image

* clone the [fabric8 git repository](https://github.com/fabric8io/fabric8) and create the vagrant image

```
git clone https://github.com/fabric8io/fabric8.git
cd fabric8
vargrant up
vagrant ssh
```

You should now have a running vagrant image running at IP address `172.28.128.4`


### Using the OpenShift CLI tools

* download and unpack a [release of OpenShift 0.5.1 or later](https://github.com/openshift/origin/releases/) for your platform and add the `openshift`, `osc` and `osadm` executables to your `PATH`
* login to OpenShift

```
osc login --server=https://172.28.128.4:8443
```

You should now be able to use the [osc CLI tool]() to work with Kubernetes and OpenShift resources:

```
osc get pods
```


### Using OpenShift from your host

On your OS X or Windows box you might want to setup these environment variables to more easily work with OpenShift from your Java code:

```
export KUBERNETES_MASTER=https://172.28.128.4:8443
sudo route -n add 172.0.0.0/8 172.28.128.4
```

The last command makes it easier to see IP address created on the vagrant box



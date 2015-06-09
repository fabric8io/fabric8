## Fabric8 Vagrant Box

We maintain a [Vagrant](http://www.vagrantup.com/downloads.html) image if you want to run OpenShift easily with Fabric8 on non-Linux platforms.

If you are on a linux box then you might want to [use the Linux instructions](setupOpenShift.html#if-you-are-on-a-linux).

### Create the vagrant image

* first clone the [fabric8 git repository](https://github.com/fabric8io/fabric8) repository

```
git clone https://github.com/fabric8io/fabric8.git
cd fabric8
```

The vagrant image is by default configured with 2 cpu cores and 4gb of memory. It is recommended to not exceed about half of your machine’s resources. In case you have plenty of resources on your machine you can increase the settings, by editing the `Vagrantfile` in the fabric8 project, you just cloned. The settings are defined in the bottom of the file:

```
    v.memory = 4096
    v.cpus = 2
```

* if the settings are okay, then the vagrant image is created and started by:

```
    vagrant up
```

* ... that process takes a little while, as resources needs to be downloaded. 

You should now have a running vagrant image running at IP address `172.28.128.4`.

You can SSH into the vagrant image using

```
    vagrant ssh
```

### Using the OpenShift CLI tools

* download and unpack a [release of OpenShift 0.5.1 or later](https://github.com/openshift/origin/releases/) for your platform (not for the vagrant image) and add the `openshift`, `osc` and `osadm` executables to your `PATH`
* login to OpenShift

```
osc login --server=https://172.28.128.4:8443
```

During the login procedure OpenShift you should allow access using insecure mode, and when prompted for an username and password type in `admin` (also `admin` as the password).

You should now be able to use the [osc CLI tool](https://github.com/openshift/origin/blob/master/docs/cli.md) to work with Kubernetes and OpenShift resources:

```
osc get pods
```


### Using OpenShift from your host

On your OS X or Windows box you might want to setup these environment variables to more easily work with OpenShift from your Java code:

```
export KUBERNETES_MASTER=https://172.28.128.4:8443
export KUBERNETES_DOMAIN=vagrant.local
export KUBERNETES_TRUST_CERT=true
sudo route -n add 172.0.0.0/8 172.28.128.4
```

The last command makes it easier to see IP address created on the vagrant box

### Looking at the OpenShift logs

If you hit any issues then try look at the logs for the `openshift` master:

```
vagrant ssh
journalctl -u openshift
```

Or you can tail the logs via 

```
vagrant ssh
journalctl -fu openshift
```

### Now setup your development machine

Once the vagrant image is running you'll have OpenShift up and configured. 

Now [Setup your local machine](setupLocalHost.html) so that your host operating system is setup to work with the vagrant image.



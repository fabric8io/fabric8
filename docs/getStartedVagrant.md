## Fabric8 Vagrant Image

This is the fastest way to get going with Fabric8 and OpenShift on your laptop.

* Download and install [VirtualBox](https://www.virtualbox.org/wiki/Downloads) 
* Download and install [Vagrant](http://www.vagrantup.com/downloads.html)
  

Now first clone the [fabric8 installer git repository](https://github.com/fabric8io/fabric8-installer) repository and type these commands:

```
git clone https://github.com/fabric8io/fabric8-installer.git
cd fabric8-installer/vagrant/openshift-latest
vagrant plugin install vagrant-hostmanager
vagrant up
```

Then follow the on screen instructions.
 
* Downloading the docker images may take a few minutes so you might want to jump ahead to the [Setting up your local machine](#setting-up-your-local-machine) then coming back here when you're done. 

* After the vagrant box is created and docker images are downloaded, the [fabric8 console](http://fabric8.io/guide/console.html) should appear at [http://fabric8.vagrant.local/](http://fabric8.vagrant.local/)
  
* When you first open your browser Chrome will say:

```
Your connection is not private
```

* Don't panic! This is to be expected.
* Click on the small `Advanced` link on the bottom left
* Now click on the link that says `Proceed to fabric8.vagrant.local (unsafe)` bottom left
* Now the browser should redirect to the login page 
* Enter `admin` and `admin`
* You should now be in the main fabric8 console! That was easy eh! :)
* Make sure you start off in the `default` namespace.

### Installing other applications

When you are on the `Apps` tab in the [fabric8 console](http://fabric8.io/guide/console.html) click on the `Run...` button. 

This will list all of the installed [OpenShift Templates](http://docs.openshift.org/latest/dev_guide/templates.html) on your installation.

* To Run any of the installed templates just click the `Run` button (the green play button).
* To install any new [OpenShift Templates](http://docs.openshift.org/latest/dev_guide/templates.html) or other Kubernetes resources just drag and drop the JSON file onto the `Apps` tab! 

### Setting up your local machine

Its useful being able to use the command line tools in OpenShift or using [fabric8 maven tooling](http://fabric8.io/guide/mavenPlugin.html) or reusing the docker daemon inside vagrant; so that all images built are accesible inside the OpenShift environment.

Follow these steps:

* [Download the recent OpenShift release binaries for your platform](https://github.com/openshift/origin/releases/)
* unpack the tarball and put the binaries on your PATH
* Set the following environment variables:

```
export KUBERNETES_MASTER=https://172.28.128.4:8443
export KUBERNETES_DOMAIN=vagrant.local
export KUBERNETES_TRUST_CERT=true
export DOCKER_HOST=tcp://vagrant.local:2375
```


* Now login to OpenShift via this command:
```
oc login --insecure-skip-tls-verify=false https://172.28.128.4:8443
```

* Enter `admin` and `admin` for user/password

ver time your token may expire and you will need to re-authenticate via:
```
oc login
```

Now to see the status of the system:
```
oc get pods
```
or you can watch from the command line via one of these commands:
```
watch oc get pods
oc get pods --watch
```

Have fun! We [love feedback](http://fabric8.io/community/)


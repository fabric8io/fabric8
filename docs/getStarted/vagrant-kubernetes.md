<img src="https://cdn.rawgit.com/fabric8io/fabric8-installer/master/img/warning.png" alt="WARNING"
     width="25" height="25">
<img src="https://cdn.rawgit.com/fabric8io/fabric8-installer/master/img/warning.png" alt="WARNING"
     width="25" height="25">
<img src="https://cdn.rawgit.com/fabric8io/fabric8-installer/master/img/warning.png" alt="WARNING"
     width="25" height="25">
<img src="https://cdn.rawgit.com/fabric8io/fabric8-installer/master/img/warning.png" alt="WARNING"
     width="25" height="25">
<img src="https://cdn.rawgit.com/fabric8io/fabric8-installer/master/img/warning.png" alt="WARNING"
     width="25" height="25">

<h2>PLEASE NOTE: Using Vagrant is now deprecated and will not be maintained</h2>

For the best local developer experience on Kubernetes and OpenShift fabric8 recommends minikube and minishift
 - [minikube](https://github.com/kubernetes/minikube)
 - [minishift](https://github.com/jimmidyson/minishift)
 - [fabric8 getting started guide](http://fabric8.io/guide/getStarted/index.html#don-t-have-a-kubernetes-cluster-yet)

---

## Fabric8 Vagrant Image for Kubernetes

This is the fastest way to get going with Fabric8 and Kubernetes on your laptop.

 ### How to vagrant up

 * Download and install [VirtualBox](https://www.virtualbox.org/wiki/Downloads)
 * Download and install [Vagrant](http://www.vagrantup.com/downloads.html)

 Now clone the
 [fabric8 installer git repository](https://github.com/fabric8io/fabric8-installer)
 repository and type these commands:

 ```sh
 $ git clone https://github.com/fabric8io/fabric8-installer.git
 $ cd fabric8-installer/vagrant/kubernetes
 ```

 Depending on your host operating system you need to install an
 additional vagrant plugin:

 * `vagrant plugin install landrush` for Linux and OS X
 * `vagrant plugin install vagrant-hostmanager` for Windows


 Now startup the Vagrant VM.

 ```sh
 vagrant up
 ```

 Note the vagrant image is by default configured with 2 CPU cores and
 4 gigs of memory. It is recommended to not exceed about half of your
 machine’s resources. In case you have plenty of resources on your
 machine you can increase the settings by editing the
 `Vagrantfile`. The settings are defined in the bottom of the file:

 ```ruby
 v.cpus = 2
 ```

 To update the RAM you can use an environment variable. For example to run the `cd-pipeline` application we recommend about 8Gb of RAM:

 ```
 export FABRIC8_VM_MEMORY=8192
 ```

 Then follow the on screen instructions.

 * You should now have a running vagrant image running at IP address
   `172.28.128.80` or at `vagrant.k8s`
 * Downloading the docker images may take a few minutes so you might
   want to jump ahead to the [Local Setup](local.html) recipe then
   coming back here when you're done.
 * After the vagrant box is created and docker images are downloaded,
   the [fabric8 console](../console.html) should appear at
   [http://172.28.128.80:8080/api/v1/proxy/namespaces/default/services/fabric8/](http://172.28.128.80:8080/api/v1/proxy/namespaces/default/services/fabric8/)
 * If you are prompted to enter a login and password then use: `admin` and `admin`
 * You should now be in the main fabric8 console! That was easy eh! :)
 * To create new Apps select the `Team Dashboard` which is usually called `default` but will be named whatever the default namespace was in your Kubernetes cluster

 ### Installing other applications

When you are in the `Runtime` perspective of a Team or Namespace in the
 [fabric8 console](http://fabric8.io/guide/console.html) click on the
 `Run...` button.

 This will list all of the installed [OpenShift Templates](http://docs.openshift.org/latest/dev_guide/templates.html)
 on your installation.

 * To Run any of the installed templates just click the `Run` button
   (the green play button).
 * To install other applications via the command line you can use the `gofabric8` binary which is inside the Vagrant image. e.g. to install the full [Fabric8 Microservices Platform Management support](../management.html) then try this command instead:

 ```
 gofabric8 deploy -y --domain=vagrant.f8 --app=management
 ```

 * To install any new
   [OpenShift Templates](http://docs.openshift.org/latest/dev_guide/templates.html)
   or other Kubernetes resources just drag and drop the JSON file onto
  the `Run...` page!
 * You can download the
   [fabric8 templates 2.2.101 distribution](http://repo1.maven.org/maven2/io/fabric8/devops/distro/distro/2.2.101/distro-2.2.101-templates.zip)
   unzip and drag the JSON files you want to install onto the
   [fabric8 console](http://fabric8.io/guide/console.html) and they
   should appear on the `Run...` page
 * You can install or upgrade application using the [helm command line tool](http://fabric8.io/guide/helm.html)
 * You can also install other OpenShift Templates or Kubernetes
   resources via the **oc** command line tool:
 ```
     kubectl create -f jsonOr YamlFileOrUrl
 ```
  * Typically the default username/password for various applications is `admin/admin` or `gogsadmin/RedHat$1`.  Try these especially for  secrets to get the pipeline to work with GOGS.

 ### Setting up your local machine

 In order to communicate with the Vagrant VM from you localhost it is
 recommended to install the kubectl client
 tool. This is explained in an extra [document](local.html).

 This is also useful using the
 [fabric8 maven tooling](../mavenPlugin.html) or reusing the docker
 daemon inside vagrant; so that all images built are accessible inside
 the OpenShift environment.

 Alternatively you can log into the Vagrant VM also via `vagrant
 ssh`. The kubectl tool is installed in the VM too.

 ### Trying a fresh image

 Note: in case you already ran the above and want to update to the
 latest vagrant image, Kubernetes and Fabric8 release you need to
 destroy and recreate the vagrant image.  You can do that using:

 ```sh
 vagrant destroy -f
 git pull
 vagrant up
 ```

 ### Troubleshooting

 Check out the [troubleshooting guide](troubleshooting.html) for more help.

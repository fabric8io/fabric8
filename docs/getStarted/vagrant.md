## Fabric8 Vagrant Image

This is the fastest way to get going with Fabric8 and OpenShift on your laptop.

Here is a [video walking you through these steps](https://vimeo.com/134408216)

<div class="row">
  <p class="text-center">
      <iframe src="https://player.vimeo.com/video/134408216" width="1000" height="562" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>
  </p>
</div>


### How to vagrant up

* Download and install [VirtualBox](https://www.virtualbox.org/wiki/Downloads)
* Download and install [Vagrant](http://www.vagrantup.com/downloads.html)

Now clone the
[fabric8 installer git repository](https://github.com/fabric8io/fabric8-installer)
repository and type these commands:

```sh
$ git clone https://github.com/fabric8io/fabric8-installer.git
$ cd fabric8-installer/vagrant/openshift
```

Depending on your host operating system you need to install an
additional vagrant plugin:

* `vagrant plugin install landrush` for Linux and OS X
* `vagrant plugin install vagrant-hostmanager` for Windows

The next steps are needed for proper routing from the host to
OpenShift services which are exposed via routes:

* **Linux**: Setup a `dnsmasq` DNS proxy locally. The detailed
  procedure depends on the Linux distribution used.  Here is the
  example for Ubuntu:

        sudo apt-get install -y resolvconf dnsmasq
        sudo sh -c 'echo "server=/vagrant.f8/127.0.0.1#10053" > /etc/dnsmasq.d/vagrant-landrush'
        sudo service dnsmasq restart

* **Windows**: Unfortunately for Windows no automatic routing for new
  services is possible. You have to add new routes manually to
  `%WINDIR%\System32\drivers\etc\hosts`. For your convenience, a set
  of routes for default Fabric8 applications has been pre-added. For
  new services look for the following line and add your new routes
  (`<service-name>.vagrant.f8`) to this file on a new line like this:

        ## vagrant-hostmanager-start id: 9a4ba3f3-f5e4-4ad4-9e80-b4045c6cf2fc
        172.28.128.4  vagrant.f8 fabric8.vagrant.f8 jenkins.vagrant.f8 .....
        172.28.128.4  myservice.vagrant.f8
        ## vagrant-hostmanager-end

* **OS X**: Nothing has to be done. OS X will automatically resolve
  all routes to `*.vagrant.f8` to your Vagrant VM. This is done via OS
  X's resolver feature (see `man 5 resolver` for details).

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
  `172.28.128.4` or at `vagrant.f8`
* Downloading the docker images may take a few minutes so you might
  want to jump ahead to the [Local Setup](local.html) recipe then
  coming back here when you're done.
* After the vagrant box is created and docker images are downloaded,
  the [fabric8 console](../console.html) should appear at
  [http://fabric8.vagrant.f8/](http://fabric8.vagrant.f8/)
* When you first open your browser Chrome will say:

        Your connection is not private

* You will want to accept the self signed cert, follow
  [these steps](./browserCertificates.html) and return here
* Enter `admin` and `admin`
* You should now be in the main fabric8 console! That was easy eh! :)
* Make sure you start off in the `default` namespace.


### Installing other applications

When you are on the `Apps` tab in the
[fabric8 console](http://fabric8.io/guide/console.html) click on the
`Run...` button.

This will list all of the installed [OpenShift Templates](http://docs.openshift.org/latest/dev_guide/templates.html)
on your installation.

* To Run any of the installed templates just click the `Run` button
  (the green play button).
* To install other applications via the command line you can use the `gofabric8` binary which is inside the Vagrant image. e.g. to install the full [fabric8 microservices platform with CI / CD support](../cdelivery.html) then try this command instead:

```
gofabric8 deploy -y --domain=vagrant.f8 --app=cd-pipeline
```

* To install any new
  [OpenShift Templates](http://docs.openshift.org/latest/dev_guide/templates.html)
  or other Kubernetes resources just drag and drop the JSON file onto
  the `Apps` tab!
* You can download the
  [fabric8 templates 2.2.101 distribution](http://repo1.maven.org/maven2/io/fabric8/devops/distro/distro/2.2.101/distro-2.2.101-templates.zip)
  unzip and drag the JSON files you want to install onto the
  [fabric8 console](http://fabric8.io/guide/console.html) and they
  should appear on the `Run...` page
* You can install or upgrade application using the [helm command line tool](http://fabric8.io/guide/helm.html)     
* You can also install other OpenShift Templates or Kubernetes
  resources via the **oc** command line tool:
```
    oc create -f jsonOr YamlFileOrUrl
```
 * Typically the default username/password for various applications is `admin/admin` or `gogsadmin/RedHat$1`.  Try these espcially for  secrets to get the pipeline to work with GOGS. 

### Setting up your local machine

In order to communicate with the Vagrant VM from you localhost it is
recommended to install the OpenShift client
tools. This is explained in an extra [document](local.html).

This is also useful using the
[fabric8 maven tooling](../mavenPlugin.html) or reusing the docker
daemon inside vagrant; so that all images built are accessible inside
the OpenShift environment.

Alternatively you can log into the Vagrant VM also via `vagrant
ssh`. The OpenShift tools (`oc`, `oadmn`) are installed in the VM,
too.

### Trying a fresh image

Note: in case you already ran the above and want to update to the
latest vagrant image, OpenShift and Fabric8 release you need to
destroy and recreate the vagrant image.  You can do that using:

```sh
vagrant destroy -f
git pull
vagrant up
```

### Troubleshooting

Check out the [troubleshooting guide](troubleshooting.html) for more help.

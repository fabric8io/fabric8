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
* To install any new
  [OpenShift Templates](http://docs.openshift.org/latest/dev_guide/templates.html)
  or other Kubernetes resources just drag and drop the JSON file onto
  the `Apps` tab!
* You can download the
  [fabric8 templates 2.2.96 distribution](http://repo1.maven.org/maven2/io/fabric8/devops/distro/distro/2.2.96/distro-2.2.96-templates.zip)
  unzip and drag the JSON files you want to install onto the
  [fabric8 console](http://fabric8.io/guide/console.html) and they
  should appear on the `Run...` page
* You can install or upgrade application using the [helm command line tool](http://fabric8.io/guide/helm.html)     
* You can also install other OpenShift Templates or Kubernetes
  resources via the **oc** command line tool:

    oc create -f jsonOr YamlFileOrUrl

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

The `oc` command can be really useful for viewing resources (pods,
services, RCs, templates etc) and deleting pods so they get recreated,
looking at logs etc.

If you add the `fabric8-installer/bin` folder to your `$PATH` then
there are a few handy shell scripts

* `oc-bash name` finds the first pod with the given name pattern and
  runs a bash shell in the docker container
* `oc-log name` finds the first pod with the given name pattern and
  shows the container's log

On your host machine or inside the vagrant image you should have
access to docker thanks to the environment variable:

```sh
export DOCKER_HOST=tcp://vagrant.f8:2375
```

So you should be able to run things like

```sh
docker ps
docker logs nameOfContainer
```

You can SSH into the vagrant image using:

```sh
vagrant ssh
```

Then you can look around.

#### Docker pull issues

If you have issues with docker pull; you can pre-download the docker
images you need before you try running the app from the OpenShift
Template or via the `Run...` button in the [console](console.html).

To pull the images for one or more templates use the following
[gofabric8](https://github.com/fabric8io/gofabric8) command:

```sh
vagrant ssh
sudo bash
gofabric8 pull logging
```

where `logging` is the name of the template to download (you can list
as many template names as you like).


#### Errors like "tcp: lookup index.docker.io: no such host"

If you shut your laptop and open it later or switch WiFi networks then
the docker daemon can struggle to connect to the upstream docker
registry to download images. There must be some issue with
Vagrant/VirtualBox DNS or something.

If this ever happens the simplest thing to do is just type this:

```sh
vagrant reload
```

This will then reload the box; you should have OpenShift running with
all your images and the DNS issue should hopefully go away!


#### Looking at the OpenShift logs

If you hit any issues then try look at the logs for the `openshift` master:

```sh
vagrant ssh
sudo journalctl -u openshift
```

Or you can tail the logs via

```sh
vagrant ssh
sudo journalctl -fu openshift
```

You can watch the docker daemon too via

```sh
vagrant ssh
sudo journalctl -fu docker
```

#### Tracing the openshift binaries download

The provision script in the VagrantFile includes a curl command to
download the openshift binaries. To observe that the download is
functioning correctly, remove the silent options from the curl
command. Replace:

```sh
curl ... -sSL https://github.com/openshift/origin/..../openshift-origin.....tar.gz | tar xzv -C /tmp/openshift
```

with

```sh
curl ... -L https://github.com/openshift/origin/..../openshift-origin.....tar.gz | tar xzv -C /tmp/openshift
```

#### Ensuring celluloid gem version not incompatible with landrush vagrant plugin

If you experience DNS issues during vagrant provisioning of the VM
then ensure that you do not have landrush vagrant plugin
version 0.18.0 installed with celluloid gem version 0.16.1.

Display the landrush version:

```sh
vagrant plugin list
```

Display the celluloid version:

```sh
export GEM_HOME=~/.vagrant.d/gems
gem list
```

If the landrush version is 0.18.0 and the celluloid version is 0.16.1
then downgrade celluloid to version 0.16.0:

```sh
export GEM_HOME=~/.vagrant.d/gems
gem uninstall celluloid -v 0.16.1
gem install celluloid -v 0.16.0
```

For further information see:
[fabric8io/fabric8#4294](https://github.com/fabric8io/fabric8/issues/4294),
[phinze/landrush#120](https://github.com/phinze/landrush/issues/120)
and
[ioquatix/rubydns#55](https://github.com/ioquatix/rubydns/issues/55).

#### Can not find / ping "vagrant.f8" from OS X

In some rare case the DNS cache can get stale when you are updating
your Vagrant or when doing restarts of the VM on OS X. In that case OS
X will refuse to resolve host addresses like `vagrant.f8`. In that
case, flushing the DNS cache helps:

        sudo dscacheutil -flushcache
        sudo killall -HUP mDNSResponder


#### Updating: tear down fabric8 and re-install after a new fabric8 release

If you want to avoid performing a `vagrant destroy && vagrant up` when a new release is available you should be able to follow these commands from within the fabric8-installer dir..

        git pull
        cd vagrant/openshift
        vagrant provision
        vagrant ssh
        sudo su
        oc login --username=admin --password=any
        oc delete all -l provider=fabric8
        oc delete templates --all
        gofabric8 deploy -y
        gofabric8 secrets -y

__NOTE__ after a `vagrant reload` you may run into the DNS cache [issue above](#can-not-find--ping-vagratf8-from-os-x)

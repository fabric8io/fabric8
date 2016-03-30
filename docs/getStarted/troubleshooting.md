### Troubleshooting

With Linux, Docker, Kubernetes / OpenShift plus optionally Vagrant / VirtualBox / VMs there are various things that can go wrong unfortunately! We really try to make stuff Just Work (tm) but now and again things fall through the cracks.

This page tries to describe all the things you can do to try figure out why things are not working.

The `oc` command can be really useful for viewing resources (pods, services, RCs, templates etc) and deleting pods so they get recreated, looking at logs etc.

You can view the current state of pods via:

    oc get pods

Or watch for when they change (e.g. start Running or become Ready or Terminate) via:

    oc get pods -w

If you have issues with the [console](../console.html) its worth checking that the `fabric8` and `router` pods are running and in a Ready state.

If things are not quite running then this can give more help

    oc describe pod fabric8-abcd

Where `fabric8-abcd` is the name of the pod you are diagnosing. 

If there's no pod but there is a [Replication Controller](../replicationControllers.html) then try this for an RC called `foo`:

    oc describe rc foo

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

    
#### Cannot access services from your browser

Sometimes services are working along with pods but you can't access them from your host. 
 
Its worth checking on OpenShift to see if there's a route for your service and what the host name is:

    oc get route
    
On OS X sometimes DNS gets a bit confused, so if things are running but you can't access them from your laptop try:

 	sudo dscacheutil -flushcache sudo killall -HUP mDNSResponder

I've sometimes seen landrush plugin for vagrant get confused too - wonder if this helps?

    vagrant landrush restart

If its DNS related you can cheat and add something like this to your /etc/hosts

    172.28.128.4 vagrant.f8 fabric8.vagrant.f8 gogs.vagrant.f8 jenkins.vagrant.f8 nexus.vagrant.f8
    
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
    
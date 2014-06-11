## Docker Containers

Fabric8 can use [docker](http://docker.io/) to create new containers; allowing you to reuse the power or docker.

Docker has all the benefits of virtualisation but without any of the costs (in IO / CPU / memory); it basically reuses a single host linux installation and lets you run separate docker containers in what appears to be a sandboxed virtual machine; when in reality its using various Linux technologies like lightweight containers, namespaces, process groups and copy-on-write file systems to simulate virtualiastion.

So Docker makes it super easy to distribute containers and create them quickly.

### Requirements

To be able to try Docker with Fabric8 you need to [install docker](https://www.docker.io/gettingstarted/#h_installation) on the machine you are running a fabric8 container.

The **DOCKER_HOST** environment variable should point to the URL to connect to docker. This is usually something like:

    export DOCKER_HOST=tcp://127.0.0.1:2375

Fabric8 uses the DOCKER_HOST environment variable to know where the Docker Remote API is located.

Once installed you should be able to run commands like:

    docker ps

The default docker container image for fabric8 is [fabric8/fabric8](https://index.docker.io/u/fabric8/fabric8/)

To install locally the main 3 docker images for fabric8 type this:

    docker pull fabric8/fabric8
    docker pull fabric8/fabric8-java
    docker pull fabric8/fabric8-tomcat

In addition it is useful to set these 2 environment variables:

    export FABRIC8_GLOBAL_RESOLVER=localip
    export FABRIC8_PROFILES=docker

The **FABRIC8_GLOBAL_RESOLVER** environment variable ensures that the IP resolver will be used; which is useful if you are not using linux and so are using Docker via some virtualisation (e.g. on OS X or Windows) where you often cannot communicate with your host machine's host name from inside the docker container.

The **FABRIC8_PROFILES** environment variable just enables the **docker** profile on startup; so you can create docker containers via the web console and can use the docker web tooling (based on [dockerui](https://github.com/crosbymichael/dockerui)).

### Using Fabric8 and Docker

Fabric8 lets you create containers using the docker container provider; which under the covers uses the [Docker Remote API](http://docs.docker.io/en/latest/reference/api/docker_remote_api/) to create/start/stop/kill containers.

You need to be careful to ensure that any docker container can connect to the IP/host names you are using to create your ZooKeeper cluster.

So create a fabric using the following command (feel free to use a different username / password and different IP address for the current machine ;):

    fabric:create --new-user admin --new-user-password admin --wait-for-provisioning --resolver manualip -m 192.168.42.1 --profile docker

Note that the **docker** profile needs to be added to the container you'll use to create new docker containers (which would be the root container if you are using your laptop).

Now when you try to create a container in the web console (hawtio) it will by default choose the docker container provider. You just need to choose a container name and one or more profiles and away you go.

Once you've created a container, it should appear if you type the following on the command line:

    docker ps

Other than that; the container should appear like any other container (e.g. like a child/ssh/openshift/cloud container).

### how to ssh into a docker container

If your docker based container doesn't start up you probably want to ssh into it and have a look around. Luckily the [fabric8/fabric8](https://index.docker.io/u/fabric8/fabric8/) container comes with sshd enabled by default.

If you type:

    docker ps

you should see the port mappings for each docker container. For example you may see something like this in the PORTS section....

    0.0.0.0:49001->22/tcp

This means that from outside the docker container; you need to use port 49001 to access port 22 inside the container. Note this number changes for each container; outside of each docker container there are different ports that forward to the 22 port.

So if the port number is 49001 then you can type something like this:

    ssh fabric8@localhost -p 49001

As Paulo [mentioned](https://github.com/paoloantinori/dockerfiles/blob/master/centos/fuse/README.md#suggestions) you may find this alias useful to avoid ssh warnings when it notices you are connecting to the same ip address that has a fingerprints different than the last time

    alias sshf="ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o PreferredAuthentications=password fabric8@localhost"
    ...
    sshf -p 49001

### how to check container logs as they start

You can also ssh into the docker container and use a regular tail as follows (see above for the **sshf** alias)

    sshf -p 49001 tail -f fabric8/data/log/karaf.log

If things are not working - or if you wanna check the logs before the containers are active (and so before you can use the web console), a neat trick is; from the host VM (which on OS X or Windows may be the boot2docker VM) do this:

    # if not on linux
    boot2docker ssh

    sudo bash
    cd /var/lib/docker/containers
    ls -al
    tail -f $container/$container-json.log

you can then tail the log output of the given container log (which is json which includes each output line).

### How to use a different docker container image

When you create a profile; you can include a **io.fabric8.docker.provider.properties** file like [this one](https://github.com/jstrachan/fabric8/blob/675/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/docker.profile/io.fabric8.docker.provider.properties#L23) which can specify the docker container image to create via the **image** property.

You can also override the default value of [fabric8/fabric8](https://index.docker.io/u/fabric8/fabric8/), for example to use **fabric8:fabric8** if you wish to use a local build of the container, and putting that value in the **io.fabric8.docker.provider.properties** file inside the **docker** profile which is used by default if there is no **io.fabric8.docker.provider.properties** file in any of the profiles being created.

You can also use the **FABRIC8_DOCKER_DEFAULT_IMAGE** environment variable if you prefer to change this outside of the configuration.

### Some docker hints and tips

If a docker container is stopped; its still around until you remove it. So remember you can type

    docker ps -a

to see them all.

To start/stop/kill/rm all containers you can use a command line this (replace "docker stop" with "docker kill" or "docker kill" as required):

    docker stop $(docker ps -a -q)

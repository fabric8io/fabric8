## Setting up your machine

To simplify the documentation and configuration script for fabric8 we are going to setup 2 local host aliases, **openshifthost** and **dockerhost** then all the instructions that follow on this site will work whether you use Docker natively on linux or you run docker in a VM or you are on a Mac or Windows and are using boot2docker.

Strictly speaking this isn't required to run fabric8 and OpenShift. e.g. on a linux box with native docker you could replace _dockerhost_ and _openshifthost_ with localhost in all the instructions and in the app/fabric8.json file :)

However doing this will make it easier to document; so if you are trying fabric8 on your laptop this is currently the simplest approach.

### Install the latest Docker

First you'll need to [install docker](https://docs.docker.com/installation/), the later the version generally the better it is!

### If you are using a Mac, Windows or other platforms

First we recommend you upgrade your boot2docker image so its the latest greatest.

    boot2docker download
    boot2docker up

If you are using boot2docker 1.3.1, you should edit /var/lib/boot2docker/profile in boot2docker VM to disable TLS, so that can use 2375 as default DOCKER_HOST port and http connection for local registry.

    boot2docker ssh
    sudo vi /var/lib/boot2docker/profile
    
and add two lines 

    DOCKER_TLS=no
    EXTRA_ARGS="--insecure-registry 192.168.59.103:5000"
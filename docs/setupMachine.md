## Setting up your machine

To simplify the documentation and configuration script for fabric8 we are going to setup 2 local host aliases, **openshifthost** and **dockerhost** then all the instructions that follow on this site will work whether you use Docker natively on linux or you run docker in a VM or you are on a Mac or Windows and are using boot2docker.

Strictly speaking this isn't required to run fabric8 and OpenShift. e.g. on a linux box with native docker you could replace _dockerhost_ and _openshifthost_ with localhost in all the instructions and in the app/fabric8.json file :)

However doing this will make it easier to document; so if you are trying fabric8 on your laptop this is currently the simplest approach.

### Install the latest Docker

First you'll need to [install docker](https://docs.docker.com/installation/), the later the version generally the better it is!

### If you are using linux with a native docker:

If you are on linux try this to define the 2 hosts to point to your localhost:

    export OPENSHIFT_HOST=127.0.0.1
    echo $OPENSHIFT_HOST openshifthost | sudo tee -a /etc/hosts
    echo $OPENSHIFT_HOST dockerhost | sudo tee -a /etc/hosts

Another option is to replace dockerhost and openshifthost in the fabric8/apps/fabric8.json file with "localhost" and use "localhost" in the 2 environment variables above and then use "localhost" whenever the documentation mentions "dockerhost" or "openshifthost".

### If you are using a Mac, Windows or other platforms

First we recommend you upgrade your boot2docker image so its the latest greatest.

    boot2docker download
    boot2docker up

When there is not a native docker, such as on a Mac or Windows when using boot2docker, there will be different IP addresses for your host machine and the boot2docker VM. So we are going to setup 2 aliases for the host (openshifthost) and docker (dockerhost):

First lets define an environment variable to point to your local IP address. You may already know this.

If not depending on your network settings and whether you are on ethernet or wifi either this:

    export OPENSHIFT_HOST=`ipconfig getifaddr en0`
    echo $OPENSHIFT_HOST

should print out your IP address or if not then try this:

    export OPENSHIFT_HOST=`ipconfig getifaddr en1`
    echo $OPENSHIFT_HOST

If you still don't have an IP address in the **OPENSHIFT_HOST** environment variable, try just running:

    ifconfig

and seeing if you can spot one. If not try figure it out yourself; e.g. look in your operating system settings.

Now lets setup the **openshifthost** alias:

    echo $OPENSHIFT_HOST openshifthost  | sudo tee -a /etc/hosts

Now lets setup the **dockerhost** alias that should point to the ip address of boot2docker:

    echo `boot2docker ip 2> /dev/null` dockerhost | sudo tee -a /etc/hosts

You probably also want to add those 2 entries to your /etc/hosts inside your boot2docker vm.

    cat /etc/hosts
    boot2docker ssh
    sudo vi /etc/hosts

Now copy/paste those 2 lines you just added and add then to the /etc/hosts on your boot2docker vm.

If you are using boot2docker 1.3.1, you should edit /var/lib/boot2docker/profile in boot2docker VM to disable TLS, so that can use 2375 as default DOCKER_HOST port and http connection for local registry.

    boot2docker ssh
    sudo vi /var/lib/boot2docker/profile
    
and add two lines 

    DOCKER_TLS=no
    EXTRA_ARGS="--insecure-registry 192.168.59.103:5000"
    

### Testing your setup

You should be able to ping the 2 host names:

    ping dockerhost
    ping openshifthost

If you are on a Mac or Windows the above should also work if you ssh into boot2docker via

    boot2docker ssh

#### Improving networking if you are on a Mac

You may not be able to ping the pod IP addresses when you've created pods. You can use the following command to be able to network from your host OS to the Pod IP addresses inside boot2docker:

    sudo route -n add  172.17.0.0/16 192.168.59.103

If you want to also be able to access POD IP ports from your Host operating system them you may also want to run the following in **boot2docker**

    sudo iptables -P FORWARD     ACCEPT

Now any Pod IP and port should be accessible both from your Host and also within the boot2docker vm

If you need more help check this guide on [iptables](https://www.frozentux.net/iptables-tutorial/iptables-tutorial.html) and [four ways to connect a docker container to a local network](http://blog.oddbit.com/2014/08/11/four-ways-to-connect-a-docker/)


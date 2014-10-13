## Running Fabric

Make sure you have [Installed OpenShift V3](installOpenShift.html) and [Setup Your Machine](setupMachine.html) first before trying the following:

#### Environment variables

Now make sure you have some environment variables setup. (You may want to place these in your **~/.bashrc** file)

    export KUBERNETES_MASTER=http://openshifthost:8080
    export DOCKER_REGISTRY=dockerhost:5000
    export DOCKER_HOST=tcp://dockerhost:2375
    export OPENSHIFT_HOST=`grep openshifthost /etc/hosts | cut -d " " -f1`

You may want to test that last expression. To see the IP address of where openshift is going to run type:

    echo $OPENSHIFT_HOST

which should look like a valid IP address and be reachable:

    ping $OPENSHIFT_HOST

### Start OpenShift

Then you can start it up; you can run this anywhere you like really:

    $ openshift start --listenAddr=$OPENSHIFT_HOST:8080

You can then use the OpenShift command line tool or the OpenShift console.

### Start fabric8

To make sure you've got the latest and greatest hawtio console try this first:

    docker pull fabric8/hawtio

Now to install fabric8 try:

    cd fabric8/apps
    openshift kube apply -c fabric8.json

Or you can just use the [fabric8.json](https://github.com/fabric8io/fabric8/blob/2.0/apps/fabric8.json) in github:

    openshift kube  apply -c https://raw.githubusercontent.com/fabric8io/fabric8/2.0/apps/fabric8.json

This will run a local docker registry and the hawtio web console.

You should be able to check if the docker registry is running OK via this command (which should return 'true'):

    curl http://$DOCKER_REGISTRY/v1/_ping

### Open the Web Console

You should be able to access the web console on your docker host at port 9282 via:

* [http://dockerhost:8484/hawtio/kubernetes/pods](http://dockerhost:8484/hawtio/kubernetes/pods)

If you are having networking issues, particularly if you are using boot2docker you can run the web console by hand:

    docker run -p 7282:8080 -it -e KUBERNETES_MASTER=$KUBERNETES_MASTER -e DOCKER_HOST=$DOCKER_HOST fabric8/hawtio

When that container starts up you should be able to view the console at

* [http://dockerhost:7282/hawtio/kubernetes/pods](http://dockerhost:7282/hawtio/kubernetes/pods)

If you want to hack on the code you can [run a local build of hawtio](https://github.com/hawtio/hawtio/blob/master/BUILDING.md#running-hawtio-against-kubernetes--openshift)

### Whats next?

So you should have now an empty OpenShift environment with the core of fabric8 installed (the web console and a local docker registry).

Now you could try:

 * [deploying an example quickstart project](http://fabric8.io/v2/mavenPlugin.html#example)


### Resetting OpenShift

At any time you can Ctrl-C or kill the _openshift_ process. This will leave around files from OpenShift and docker containers. So you can clear down your system via:

```
    rm -rf openshift.local.*
    docker kill $(docker ps -q)
    docker rm $(docker ps -a -q)
```

### Other Resources

#### View a demo

To help you get started, you could watch one of the demos in the  <a class="btn btn-success" href="https://vimeo.com/album/2635012">JBoss Fuse and JBoss A-MQ demo album</a>

For example, try the <a class="btn btn-success" href="https://vimeo.com/80625940">JBoss Fuse 6.1 Demo</a>

#### Try QuickStarts

New users to Fabric8 should try the [QuickStarts](/gitbook/quickstarts.html).

#### Read the documentation

Check out the [Overview](/gitbook/overview.html) and [User Guide](/gitbook/index.html).

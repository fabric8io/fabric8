### Example

The following example shows you how to build and push a docker image to Kubernetes and deploy it and then use it.

Make sure you have followed the [Get Started Guide](http://fabric8.io/v2/getStarted.html) so you should have things running.

You should be able to check if the docker registry is running OK via this command (which should return 'true'):

    curl http://$DOCKER_REGISTRY/v1/_ping

Or you can use the [ping-registry.sh](https://github.com/fabric8io/fabric8/blob/master/bin/ping-registry.sh) scriptwhich will check that you have your **DOCKER_REGISTRY** environment variable setup correctly to point to a valid docker registry:

    ping-registry.sh

If either of those return **true** you are ready to build a quickstart!

#### Build the camel-servlet web application

From the distribution or source code perform these commands to push the docker image:

    git clone https://github.com/fabric8io/quickstarts.git
    cd quickstarts

Now lets navigate to the specific quickstart:

    cd quickstarts/war/camel-servlet
    mvn clean install docker:build
    docker push $DOCKER_REGISTRY/quickstart/war-camel-servlet:2.0-SNAPSHOT

Now lets deploy the image into the Kubernetes environment:

    mvn fabric8:run

You should now be able to view the running web application at [http://dockerhost:9901/war-camel-servlet-2.0-SNAPSHOT/](http://dockerhost:9901/war-camel-servlet-2.0-SNAPSHOT/)

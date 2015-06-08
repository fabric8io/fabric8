### Example

The following example shows you how to build a docker image and run it on Kubernetes.

Make sure you have followed the [Get Started Guide](http://fabric8.io/guide/getStarted.html) so you should have things running and you have [setup your local machine](setupLocalHost.html).

Also check out [how to develop applications locally](developLocally.html).

#### Build the camel-servlet web application

From the distribution or source code perform these commands to push the docker image:

    git clone https://github.com/fabric8io/quickstarts.git
    cd quickstarts

Now let's navigate to the specific quickstart:

    cd quickstarts/war/camel-servlet
    mvn clean install docker:build 

Now let's deploy the image into the Kubernetes environment:

    mvn fabric8:json fabric8:apply

You should now be able to view the quickstart in the fabric8 console.  On the Services tab you will see the camel-servlet URL which will take you to the running example.

## Get Started with Fabric8 and Jube

First you need to get the latest [Jube](jube.html) by [downloading jube-2.0.9-image.zip](http://central.maven.org/maven2/io/fabric8/jube/images/jube/jube/2.0.9/jube-2.0.9-image.zip) and unzipping it:

    curl -O http://central.maven.org/maven2/io/fabric8/jube/images/jube/jube/2.0.9/jube-2.0.9-image.zip
    mkdir jube-2.0.9-image
    cd jube-2.0.9-image
    unzip ../jube-2.0.9-image.zip

You can then startup Jube via:

    cd jube-2.0.9-image
    ./run.sh

If your operating system doesn't have the executable flag set on the run script; try

    chmod +x *.sh *.bat bin/*

To stop Jube hit **Ctrl-C**.

If you want to run Jube in the background you can run **./start.sh** then you can run **./stop.sh** to stop it or run **./status.sh** to see if its still running.

### Setting environment variables

The following environment variables are used by various [Tools](http://fabric8.io/v2/tools.html) such as the [Console](console.html), [Maven Plugin](http://fabric8.io/v2/mavenPlugin.html), the [Forge Addons](http://fabric8.io/v2/forge.html) and the [java libraries](javaLibraries.html):

    export KUBERNETES_MASTER=http://localhost:8585/
    export FABRIC8_CONSOLE=http://localhost:8585/hawtio/

Once you have set those in your shell (or ~/.bashrc) you can then use all the fabric8 tools against your local Jube node.

### Using the web console

Once Jube has started up you should be able to open the [Console](console.html) at [http://localhost:8585/hawtio/](http://localhost:8585/hawtio/) to view the kubernetes system. You can then view these tabs:

 * [Pods tab](http://localhost:8585/hawtio/kubernetes/pods) views all the available [pods](pods.html) in your kubernetes environment
 * [Replication Controllers tab](http://localhost:8585/hawtio/kubernetes/replicationControllers) views all the available [replication controllers](replicationControllers.html) in your kubernetes environment
 * [Services tab](http://localhost:8585/hawtio/kubernetes/services) views all the available [services](services.html) in your kubernetes environment

Jube implements the [Kubernetes](http://kubernetes.io/) REST API so you can use any kubernetes tools with Jube such as the [Console](console.html) or [Forge Addons](http://fabric8.io/v2/forge.html) to work with [pods](pods.html), [replication controllers](replicationControllers.html) or [services](services.html) - provided that any docker images referenced in the pods and pod templates have a suitable [image zip](http://fabric8.io/jube/imageZips.html). For more detail on this see the [Jube Goals](http://fabric8.io/jube/goals.html) details on [Jube Image Zips](http://fabric8.io/jube/imageZips.html) and check out the [differences between Jube and Kubernetes](http://fabric8.io/jube/differences.html)

### Troubleshooting

If maven builds fail it may be you have not setup the [environment variables](getStartedJube.html#setting-environment-variables) correctly; see above.

Otherwise check out the [Jube Troubleshooting Guide](http://fabric8.io/jube/troubleshooting.html) for details of how to check the logs and web console.

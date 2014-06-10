camel-spring-boot: demonstrates using Camel with Spring Boot in a Java Container
================================================================================

## What is it?

This example demonstrates how you can use Apache Camel with Spring Boot in a [Java Container](http://fabric8.io/gitbook/javaContainer.html)

## System requirements

Before building and running this quick start you need:

* Maven 3.0.4 or higher
* JDK 1.7
* Fabric8


## Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To build from the source code:

1. Change your working directory to `quickstarts\spring-boot\camel-spring-boot` directory.
1. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `quickstarts\spring-boot\camel-spring-boot` directory.
1. Run `mvn fabric8:deploy` to upload the quickstart to the fabric container.

If you run the `fabric:deploy` command for the first then, it will ask you for the username and password to login the fabric container.
And then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).


## How to run this example

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `quickstarts-camel.spring.boot` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile quickstarts-camel.spring.boot root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.

### How to try this example

Login to the web console

    http://localhost:8181/hawtio/index.html

In the web console you should see a list of containers. Click the open button for the `mychild` container, which opens a new tab.

You should now see the console for your container and view the Camel tab to view the currently running camel routes


## Undeploy this example

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
2. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild


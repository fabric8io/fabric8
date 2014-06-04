cbr: Demonstrates how to use the content-based router pattern in Camel to send a message
======================================================

## What is it?

This quick start shows how to use Apache Camel, and its OSGi integration to dynamically route messages to new or updated OSGi bundles. This allows you to route to newly deployed services at runtime without impacting running services.

This quick start combines use of the Camel Recipient List, which allows you to at runtime specify the Camel Endpoint to route to, and use of the Camel VM Component, which provides a SEDA queue that can be accessed from different OSGi bundles running in the same Java virtual machine.

## System requirements

Before building and running this quick start you need:

* Maven 3.0.4 or higher
* JDK 1.7
* Fabric8


## How to run this example

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `example-quickstarts-cbr` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile example-quickstarts-cbr root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.
1. Log into the `mychild` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect mychild

1. View the container log using the `log:tail` command as follows:

        log:tail


### How to try this example

To use the application be sure to have deployed the quickstart in fabric8 as described above. 

1. As soon as the Camel route has been started, you will see a directory `instances/mychild/work/cbr/input` in your Fabric8 installation.
2. Copy the files you find in this quick start's `src/main/resources/data` directory to the newly created `instances/mychild/work/cbr/input`
directory.
3. Wait a few moments and you will find the same files organized by country under the `instances/mychild/work/cbr/output` directory.
  * `order1.xml` in `instances/mychild/work/cbr/output/others`
  * `order2.xml` and `order4.xml` in `instances/mychild/work/cbr/output/uk`
  * `order3.xml` and `order5.xml` in `instances/mychild/work/cbr/output/us`
4. Use `log:display` to check out the business logging.
        Receiving order order1.xml
        Sending order order1.xml to another country
        Done processing order1.xml


## Undeploy this example

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
2. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild


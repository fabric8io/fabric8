errors: demonstrates exception handling in Camel
===================================

## What is it?

This quickstart demonstrates how to handle exceptions that occur while routing messages with Camel.

This quickstart show you how to add a default error handler to your Camel context for all uncaught exceptions.
Additionally, it will show you how to add exception handling routines for dealing with specific exception types.


## System requirements

Before building and running this quick start you need:

* Maven 3.0.4 or higher
* JDK 1.7
* Fabric8


## How to run this example

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `example-quickstarts-errors` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile example-quickstarts-errors root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.
1. Log into the `mychild` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect mychild

1. View the container log using the `log:tail` command as follows:

        log:tail


### How to try this example

To use the application be sure to have deployed the quickstart in fabric8 as described above. Successful deployment will create and start a Camel route in fabric8.

1. As soon as the Camel route has been started, you will see a directory `instances/mychild/work/errors/input` in your fabric8 installation.
2. Copy the file you find in this quick start's `src/main/resources/data` directory to the newly created
`instances/mychild/work/errors/input` directory.
4. Wait a few moments and you will find the files in directories under `instances/mychild/work/errors`:
  * `order4.xml` will always end up in the `instances/mychild/work/errors/validation` directory
  * other files will end up in `instances/mychild/work/errors/done` or `instances/mychild/work/errors/deadletter` depending on the runtime exceptions that occur
5. Use `log:display` to check out the business logging - the exact output may look differently because the 'unexpected runtime exception...' happen randomly

        Processing order4.xml
        Order validation failure: order date 2012-03-04 should not be a Sunday
        Validation failed for order4.xml - moving the file to work/errors/validation
        Processing order5.xml
        An unexcepted runtime exception occurred while processing order5.xml
        Done processing order5.xml
        ...


## Undeploy this example

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
2. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild


# Camel error handler QuickStart

This quickstart demonstrates how to handle exceptions that occur while routing messages with Camel.

This quickstart show you how to add a default error handler to your Camel context for all uncaught exceptions. Additionally, it will show you how to add exception handling routines for dealing with specific exception types.

The example is implemented using the following two Camel routes

* mainRoute
* dlcRoute

The routes is illustrated in the following diagram

![Camel EIPs diagram](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/camel-errorhandler-diagram.png)

The entry is the mainRoute which pickup incoming order files. The files is processed and validated by Java beans. If any of the orders is invalid an exception is thrown.

The orders can fail for two reasons:

* validation failed, then an `OrderValidationException` is thrown by the Java beans
* other kind of failures, then any kind of `Exception` is thrown by the Java beans

If an `OrderValidationException` was thrown, then this is handled specially by Camel, as we have a `onException(OrderValidationException.class)` that handles this exception specially, by writing to the log, and saving the order file into the `work/errors/validation` directory.

If any other exception was thrown, then Camels Dead Letter Channel error handler deals with this by routing the message to the `dlcRoute` which also writes to the log, but saves the order xml file into the `work/errors/deadletter` directory.

Notice the Java bean implementations has *random* logic that causes different kind of exceptions being thrown at each run. So running this example can yield different outputs.

## Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To build from the source code:

1. Change your working directory to `quickstarts/beginner/camel-errorhandler` directory.
1. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `quickstarts/beginner/camel-errorhandler` directory.
1. Run `mvn fabric8:deploy` to upload the quickstart to the fabric container.

If you run the `fabric:deploy` command for the first then, it will ask you for the username and password to login the fabric container.
And then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).


## How to run this example from shell

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `quickstarts-beginner-camel.errorhandler` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile quickstarts-beginner-camel.errorhandler root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.
1. Log into the `mychild` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect mychild

1. View the container log using the `log:tail` command as follows:

        log:tail


### How to try this example from shell

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


## Undeploy this example from shell

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
2. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild


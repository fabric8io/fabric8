camel-errorhandler: demonstrates how to handle exceptions in Camel.
======================================================
Author: Fuse Team  
Level: Beginner  
Technologies: Camel  
Summary: This quickstart demonstrates how to handle exceptions that can occur while routing messages with Camel.  
Target Product: Fuse  
Source: <https://github.com/jboss-fuse/quickstarts>  

This quickstart show you how to add a default error handler to your Camel context for all uncaught exceptions. Additionally, it will show you how to add exception handling routines for dealing with specific exception types.

The example is implemented using the following two Camel routes

* mainRoute
* dlcRoute

The routes is illustrated in the following diagram

![Camel EIPs diagram](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/camel-errorhandler-diagram.jpg)

The entry is the mainRoute which pickup incoming order files. The files is processed and validated by Java beans. If any of the orders is invalid an exception is thrown.

The orders can fail for two reasons:

* validation failed, then an `OrderValidationException` is thrown by the Java beans
* other kind of failures, then any kind of `Exception` is thrown by the Java beans

If an `OrderValidationException` was thrown, then this is handled specially by Camel, as we have a `onException(OrderValidationException.class)` that handles this exception specially, by writing to the log, and saving the order file into the `work/errors/validation` directory.

If any other exception was thrown, then Camels Dead Letter Channel error handler deals with this by routing the message to the `dlcRoute` which also writes to the log, but saves the order xml file into the `work/errors/deadletter` directory.

Notice the Java bean implementations has *random* logic that causes different kind of exceptions being thrown at each run. So running this example can yield different outputs.

## Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To try the example you do not need to build from source first. Although building from source allows you to modify the source code, and re-deploy the changes to fabric. See more details on the fabric8 website about the [developer workflow](http://fabric8.io/gitbook/developer.html).

To build from the source code:

1. Change your working directory to `quickstarts/beginner/camel-errorhandler` directory.
1. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `quickstarts/beginner/camel-errorhandler` directory.
1. Run `mvn fabric8:deploy` to upload the quickstart to the fabric container.

If you run the `fabric:deploy` command for the first then, it will ask you for the username and password to login the fabric container.
And then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).


## How to run this example

The following information is divded into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the command line shell

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `quickstarts-beginner
quickstarts/beginner/-camel.errorhandler` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile quickstarts-beginner
quickstarts/beginner/-camel.errorhandler root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.
1. Log into the `mychild` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect mychild

1. View the container log using the `log:tail` command as follows:

        log:tail

To exit the tail logger, press Ctrl-D. And to logout from the `mychild` container, then use the `exit` command, which returns back to the `root` container.

### Using the web console

You can deploy and run this example from the web console, as follows

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Login the web console
1. Click the Wiki button in the navigation bar
1. Select `quickstarts` --> `beginner` --> `camel.errorhandler`
1. Click the `New` button in the top right corner
1. In the Create New Container page, enter `mychild` in the Container Name field, and click the *Create and start container* button


## How to try this example from shell

The following information is divded into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the command line shell

To use the application be sure to have deployed the quickstart in fabric8 as described above. Successful deployment will create and start a Camel route in fabric8.

1. As soon as the Camel route has been started, you will see a directory `instances/mychild/work/errors/input` in your fabric8 installation.
1. Copy the file you find in this quick start's `src/main/fabric8/data` directory to the newly created
`instances/mychild/work/errors/input` directory.
1. Wait a few moments and you will find the files in directories under `instances/mychild/work/errors`:
  * `order4.xml` will always end up in the `instances/mychild/work/errors/validation` directory
  * other files will end up in `instances/mychild/work/errors/done` or `instances/mychild/work/errors/deadletter` depending on the runtime exceptions that occur
1. Use `log:display` to check out the business logging - the exact output may look differently because the 'unexpected runtime exception...' happen randomly

        Processing order4.xml
        Order validation failure: order date 2012-03-04 should not be a Sunday
        Validation failed for order4.xml - moving the file to work/errors/validation
        Processing order5.xml
        An unexcepted runtime exception occurred while processing order5.xml
        Done processing order5.xml
        ...

### Using the web console

This example comes with sample data which you can use to try this example

1. Login the web console
1. Click the Runtime button in the navigation bar
1. Select the `mychild` container in the containers list, and click the *open* button right next to the container name.
1. A new window opens and connects to the container. Click the *Camel* button in the navigation bar. 
1. In the Camel tree, expand the `Endpoints` tree, and select the second last node, which is `file://work/errors/input`, and click the *Send* button in the sub navigation bar.
1. Click the *Choose* button and mark [x] for the five `data/order1.xml` ... `data/order5.xml` files.
1. Click the *Send 5 files* button in the top right corner
1. In the Camel tree, click the `Routes` node which then lists metrics for all the routes. The `mainRoute` route should complete 5 messages, and the `dlcRoute` completes 1 message. Depending on randomness, then `mainRoute` should handle 1 or more exceptions, which is the number in the `Failed Handled #` column.
1. In the Camel tree, you can click each individual route, and click the `Diagram` button in the sub navigation bar, to see a visual representation of the given route.  
1. You can click the *Log* button the navigation bar to see the business logging.

An illustration of step #8 is shown in the figure below:

![Camel EIPs diagram](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/camel-errorhandler-route-table.png)


## Undeploy this example

The following information is divded into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the command line shell

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
1. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild

### Using the web console

To stop and undeploy the example in fabric8:

1. In the web console, click the *Runtime* button in the navigation bar.
1. Select the `mychild` container in the *Containers* list, and click the *Stop* button in the top right corner


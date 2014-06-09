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


## How to run this example from web console

You can deploy and run this example from the web console, as follows

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
2. Login the web console
3. Click the Wiki button in the navigation bar
2. Select `example` --> `quickstarts` --> `beginner` --> `camel.errorhandler`
3. Click the `New` button in the top right corner
4. In the Create New Container page, enter `mychild` in the Container Name field, and click the *Create and start container* button

### How to try this example from web console


This example comes with sample data which you can use to try this example

1. Login the web console
2. Click the Runtime button in the navigation bar
3. Select the `mychild` container in the containers list, and click the *open* button right next to the container name.
4. A new window opens and connects to the container. Click the *Camel* button in the navigation bar. 
5. In the Camel tree, expand the `Endpoints` tree, and select the second last node, which is `file://work/errors/input`, and click the *Send* button in the sub navigation bar.
6. Click the *Choose* button and mark [x] for the five `data/order1.xml` ... `data/order5.xml` files.
7. Click the *Send 5 files* button in the top right corner
8. In the Camel tree, click the `Routes` node which then lists metrics for all the routes. The `mainRoute` route should complete 5 messages, and the `dlcRoute` completes 1 message. Depending on randomness, then `mainRoute` should handle 1 or more exceptions, which is the number in the `Failed Handled #` column.
9. In the Camel tree, you can click each individual route, and click the `Diagram` button in the sub navigation bar, to see a visual representation of the given route.  
10. You can click the *Log* button the navigation bar to see the business logging.

An illustration of step #8 is shown in the figure below:

![Camel EIPs diagram](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/camel-errorhandler-route-table.png)


### Undeploy this example from web console

To stop and undeploy the example in fabric8:

1. In the web console, click the *Runtime* button in the navigation bar.
2. Select the `mychild` container in the *Containers* list, and click the *Stop* button in the top right corner


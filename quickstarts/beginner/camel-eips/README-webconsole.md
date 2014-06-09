# Camel EIPs QuickStart

This quickstart demonstrates how to combine multiple EIPs to solve integration problems.

In this example, an orders file containing several orders for zoos around the world is sent to us. We first want to make sure we retain a copy of the original file. This is done using the Wiretap EIP. After saving the original, we want to split the file up into the individual orders. This is done using the Splitter EIP. Then we want to store the orders in separate directories by geographical region. This is done using a Recipient List EIP. Finally, we want to filter out the orders that contain more than 100 animals and generate a message for the strategic account team. This is done using a Filter EIP.

The example is implemented using the following four Camel routes

* mainRoute
* wireTapRoute
* splitterRoute
* filterRoute

The routes is illustrated in the following diagram

![Camel EIPs diagram](https://github.com/fabric8io/fabric8/tree/master/docs/images/camel-eips-diagram.png)


### Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To build from the source code:

1. Change your working directory to `eips` directory.
2. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `eips` directory.
1. Run `mvn fabric8:deploy` to upload the quickstart to the fabric container.

If you run the `fabric:deploy` command for the first then, it will ask you for the username and password to login the fabric container.
And then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).


### How to run this example from web console

You can deploy and run this example at the console command line, as follows:


1. It is assumed that you have already created a fabric and are logged into a container called `root`.
2. Login the web console
3. Click the Wiki button in the navigation bar
2. Select `example` --> `quickstarts` --> `beginner` --> `camel.eips`
3. Click the `New` button in the top right corner
4. In the Create New Container page, enter `mychild` in the Container Name field, and click the *Create and start container* button


### How to try this example from web console

This example comes with sample data which you can use to try this example

1. Login the web console
2. Click the Runtime button in the navigation bar
3. Select the `mychild` container in the containers list, and click the *open* button right next to the container name.
4. A new window opens and connects to the container. Click the *Camel* button in the navigation bar.
5. In the Camel tree, expand the `Endpoints` tree, and select the last node, which is `file://work/eip/input`, and click the *Send* button in the sub navigation bar.
6. Click the *Choose* button and mark [x] for the `data/orders.xml` file.
7. Click the *Send the file* button in the top right corner
8. In the Camel tree, click the `Routes` node which then lists metrics for all the routes. The `mainRoute`, `splitterRouter`, and `wireTap` route should all complete 1 message, and the `filterRoute` completes 6 messages.
9. In the Camel tree, you can click each individual route, and click the `Diagram` button in the sub navigation bar, to see a visual representation of the given route.  
9. You can click the *Log* button the navigation bar to see the business logging.


### Undeploy this example from web console

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
2. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild


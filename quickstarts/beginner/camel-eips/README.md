camel-eips: Demonstrates how to combine multiple EIPS in Camel
======================================================
Author: Fuse Team  
Level: Beginner  
Technologies: Camel  
Summary: This quickstart demonstrates how to combine multiple EIPs in Camel in order to solve integration problems.  
Target Product: Fuse  
Source: <https://github.com/jboss-fuse/quickstarts>  

In this example, an orders file containing several orders for zoos around the world is sent to us. We first want to make sure we retain a copy of the original file. This is done using the Wiretap EIP. After saving the original, we want to split the file up into the individual orders. This is done using the Splitter EIP. Then we want to store the orders in separate directories by geographical region. This is done using a Recipient List EIP. Finally, we want to filter out the orders that contain more than 100 animals and generate a message for the strategic account team. This is done using a Filter EIP.

The example is implemented using the following four Camel routes

* mainRoute
* wireTapRoute
* splitterRoute
* filterRoute

The routes is illustrated in the following diagram

![Camel EIPs diagram](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/camel-eips-diagram.jpg)


### Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To try the example you do not need to build from source first. Although building from source allows you to modify the source code, and re-deploy the changes to fabric. See more details on the fabric8 website about the [developer workflow](http://fabric8.io/gitbook/developer.html).

To build from the source code:

1. Change your working directory to `quickstarts/beginner/camel-eips` directory.
1. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `quickstarts/beginner/camel-eips` directory.
1. Run `mvn fabric8:deploy` to upload the quickstart to the fabric container.

If you run the `fabric:deploy` command for the first then, it will ask you for the username and password to login the fabric container.
And then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).

## How to run this example

The following information is divded into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the command line shell

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `quickstarts-beginner
quickstarts/beginner/-camel.eips` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile quickstarts-beginner
quickstarts/beginner/-camel.eips root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.
1. Log into the `mychild` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect mychild

1. View the container log using the `log:tail` command as follows:

        log:tail

To exit the tail logger, press Ctrl-D. And to logout from the `mychild` container, then use the `exit` command, which returns back to the `root` container.

### Using the web console

You can deploy and run this example from the web console, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Login the web console
1. Click the Wiki button in the navigation bar
1. Select `quickstarts` --> `beginner` --> `camel.eips`
1. Click the `New` button in the top right corner
1. In the Create New Container page, enter `mychild` in the Container Name field, and click the *Create and start container* button


## How to try this example from shell

The following information is divded into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the command line shell

To use the application be sure to have deployed the quickstart in fabric8 as described above. Successful deployment will create and start a Camel route in fabric8.

1. As soon as the Camel route has been started, you will see a directory `instances/mychild/work/eip/input` in your fabric8 installation.
1. Copy the file you find in this example's `src/main/fabric8/data` directory to the newly created `instances/mychild/work/eip/input`
directory.
1. Wait a few moments and you will find multiple files organized by geographical region under `instances/mychild/work/eip/output`:
** `2012_0003.xml` and `2012_0005.xml` in `instances/mychild/work/eip/output/AMER`
** `2012_0020.xml` in `instances/mychild/work/eip/output/APAC`
** `2012_0001.xml`, `2012_0002.xml` and `2012_0004.xml` in `instances/mychild/work/eip/output/EMEA`
1. Use `log:display` on the shell to check out the business logging.
        [main]    Processing orders.xml
        [wiretap]  Archiving orders.xml
        [splitter] Shipping order 2012_0001 to region EMEA
        [splitter] Shipping order 2012_0002 to region EMEA
        [filter]   Order 2012_0002 is an order for more than 100 animals
        ...

### Using the web console

This example comes with sample data which you can use to try this example

1. Login the web console
1. Click the Runtime button in the navigation bar
1. Select the `mychild` container in the containers list, and click the *open* button right next to the container name.
1. A new window opens and connects to the container. Click the *Camel* button in the navigation bar.
1. In the Camel tree, expand the `Endpoints` tree, and select the last node, which is `file://work/eip/input`, and click the *Send* button in the sub navigation bar.
1. Click the *Choose* button and mark [x] for the `data/orders.xml` file.
1. Click the *Send the file* button in the top right corner
1. In the Camel tree, click the `Routes` node which then lists metrics for all the routes. The `mainRoute`, `splitterRouter`, and `wireTap` route should all complete 1 message, and the `filterRoute` completes 6 messages.
1. In the Camel tree, you can click each individual route, and click the `Diagram` button in the sub navigation bar, to see a visual representation of the given route.
1. You can click the *Log* button the navigation bar to see the business logging.


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


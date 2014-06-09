# Camel EIPs QuickStart

This quickstart demonstrates how to combine multiple EIPs to solve integration problems.

In this example, an orders file containing several orders for zoos around the world is sent to us. We first want to make sure we retain a copy of the original file. This is done using the Wiretap EIP. After saving the original, we want to split the file up into the individual orders. This is done using the Splitter EIP. Then we want to store the orders in separate directories by geographical region. This is done using a Recipient List EIP. Finally, we want to filter out the orders that contain more than 100 animals and generate a message for the strategic account team. This is done using a Filter EIP.

The example is implemented using the following four Camel routes

* mainRoute
* wireTapRoute
* splitterRoute
* filterRoute

The routes is illustrated in the following diagram

![Camel EIPs diagram](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/camel-eips-diagram.png)


### Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To build from the source code:

1. Change your working directory to `eip` directory.
2. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `eip` directory.
1. Run `mvn fabric8:deploy` to upload the quickstart to the fabric container.

If you run the `fabric:deploy` command for the first then, it will ask you for the username and password to login the fabric container.
And then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).


### How to run this example from shell

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `quickstarts-eip` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile quickstarts-eip root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.
1. Log into the `mychild` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect mychild

1. View the container log using the `log:tail` command as follows:

        log:tail


### How to try this example from shell

To use the application be sure to have deployed the quickstart in fabric8 as described above. Successful deployment will create and start a Camel route in fabric8.

1. As soon as the Camel route has been started, you will see a directory `instances/mychild/work/eip/input` in your fabric8 installation.
2. Copy the file you find in this example's `src/main/resources/data` directory to the newly created `instances/mychild/work/eip/input`
directory.
3. Wait a few moments and you will find multiple files organized by geographical region under `instances/mychild/work/eip/output`:
** `2012_0003.xml` and `2012_0005.xml` in `instances/mychild/work/eip/output/AMER`
** `2012_0020.xml` in `instances/mychild/work/eip/output/APAC`
** `2012_0001.xml`, `2012_0002.xml` and `2012_0004.xml` in `instances/mychild/work/eip/output/EMEA`
4. Use `log:display` on the shell to check out the business logging.
        [main]    Processing orders.xml
        [wiretap]  Archiving orders.xml
        [splitter] Shipping order 2012_0001 to region EMEA
        [splitter] Shipping order 2012_0002 to region EMEA
        [filter]   Order 2012_0002 is an order for more than 100 animals
        ...


### Undeploy this example from shell

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
2. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild


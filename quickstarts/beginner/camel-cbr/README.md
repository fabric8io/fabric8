# Camel Content Based Router QuickStart

This quickstart shows how to use Apache Camel to route messages using the Content Based Router (cbr) pattern.

This example is implemented using solely the XML DSL (there is no Java code). The source code is provided in the following XML file `src/main/resources/OSGI-INF/blueprint/cbr.xml`, which can be viewed from [github](https://github.com/fabric8io/fabric8/blob/master/quickstarts/beginner/camel-cbr/src/main/resources/OSGI-INF/blueprint/cbr.xml).

This example pickup incoming XML files, and depending on the content of the XML files, they are routed to different endpoints, as shown in figure below.

![Camel CBR diagram](https://github.com/fabric8io/fabric8/tree/master/docs/images/camel-cbr-diagram.png)

The example comes with sample data, making it easy to try the example yourself.

### Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To build from the source code:

1. Change your working directory to `cbr` directory.
1. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `cbr` directory.
1. Run `mvn fabric8:deploy` to upload the quickstart to the fabric container.

If you run the `fabric:deploy` command for the first then, it will ask you for the username and password to login the fabric container.
And then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).

### How to run this example from shell

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


### How to try this example from shell

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


### Undeploy this example from shell

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
2. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild


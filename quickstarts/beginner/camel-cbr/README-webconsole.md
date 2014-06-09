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

### How to run this example from web console

You can deploy and run this example from the web console, as follows

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
2. Login the web console
3. Click the Wiki button in the navigation bar
2. Select `example` --> `quickstarts` --> `beginner` --> `camel.cbr`
3. Click the `New` button in the top right corner
4. In the Create New Container page, enter `mychild` in the Container Name field, and click the *Create and start container* button


### How to try this example from web console

This example comes with sample data which you can use to try this example

1. Login the web console
2. Click the Runtime button in the navigation bar
3. Select the `mychild` container in the containers list, and click the *open* button right next to the container name.
4. A new window opens and connects to the container. Click the *Camel* button in the navigation bar.
5. In the Camel tree, expand the `Endpoints` tree, and select the first node, which is `file://work/cbr/input`, and click the *Send* button in the sub navigation bar.
6. Click the *Choose* button and mark [x] for the five `data/order1.xml` ... `data/order5.xml` files.
7. Click the *Send 5 files* button in the top right corner
8. In the Camel tree, expand the `Routes` node, and select the first node, which is the `cbr-route` route. And click the *Diagram* button to see a visual representation of the route.
9. Notice the numbers in the diagram, which illustrate that 5 messages has been processed, of which 2 were from UK, 2 from US, and 1 others. 
10. You can click the *Log* button the navigation bar to see the business logging.


### Undeploy this example from web console

To stop and undeploy the example in fabric8:

1. In the web console, click the *Runtime* button in the navigation bar.
2. Select the `mychild` container in the *Containers* list, and click the *Stop* button in the top right corner


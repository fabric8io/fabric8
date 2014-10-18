# Camel Spring QuickStart

This quickstarts run in a Java standalone container, using Spring with Apache Camel.

This example is implemented using solely a Spring XML file (there is no Java code). The source code is provided in the following XML file `src/main/resources/META-INF/spring/camel-context.xml`, which can be viewed from [github](https://github.com/fabric8io/fabric8/blob/master/quickstarts/java/camel-spring/src/main/resources/META-INF/spring/camel-context.xml).

This example pickup incoming XML files, and depending on the content of the XML files, they are routed to different endpoints, as shown in figure below.

![Camel Spring diagram](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/camel-spring-diagram.jpg)

The example comes with sample data, making it easy to try the example yourself.

### Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To try the example you do not need to build from source first. Although building from source allows you to modify the source code, and re-deploy the changes to fabric. See more details on the fabric8 website about the [developer workflow](http://fabric8.io/gitbook/developer.html).

To build from the source code:

1. Change your working directory to `quickstarts/java/camel-spring` directory.
1. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `quickstarts/java/camel-spring` directory.
1. Run `mvn fabric8:deploy` to upload the quickstart to the fabric container.

If you run the `fabric:deploy` command for the first then, it will ask you for the username and password to login the fabric container.
And then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).


## How to run this example

The following information is divded into three sections, whether you are using the command line shell in fabric, or using the web console, or run the example from the source code.

### Using the command line shell

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `quickstarts-java-camel.spring` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile quickstarts-java-camel.spring root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.


### Using the web console

You can deploy and run this example from the web console, as follows

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Login the web console
1. Click the Wiki button in the navigation bar
1. Select `quickstarts` --> `java` --> `camel.spring`
1. Click the `New` button in the top right corner
1. In the Create New Container page, enter `mychild` in the Container Name field, and click the *Create and start container* button

### From the source code

Follow the instructions from the _Building this example_ section, and after you have built the source code run the following command:

1. Run `mvn camel:run` to run the example as a standalone Spring based Camel application.

Running outside fabric means that you do not have the fabric web console or fabric server to manage the application. You may want to use `mvn camel:run` during development and to quickly try your code changes.


## How to try this example

The following information is divded into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the command line shell

To use the application be sure to have deployed the quickstart in fabric8 as described above. 

1. As soon as the Camel route has been started, you will see a directory `instances/mychild/src/data` in your Fabric8 installation.
1. Copy the files you find in this quick start's `src/data` directory to the newly created `processes/mychild/src/data`
directory.
1. Wait a few moments and you will find the same files organized by country under the `instances/mychild/target/messges` directory. There should be 1 message in the uk folder, and another message in others. 

### Using the web console

This example comes with sample data which you can use to try this example

1. Login the web console
1. Click the Runtime button in the navigation bar
1. Select the `mychild` container in the containers list, and click the *open* button right next to the container name.
1. A new window opens and connects to the container. Click the *Camel* button in the navigation bar.
1. In the Camel tree, expand the `Endpoints` tree, and select the first node, which is `file://src/data`, and click the *Send* button in the sub navigation bar.
1. Click the *Choose* button and mark [x] for the five `data/message1.xml` ... `message2.xml` files.
1. Click the *Send 2 files* button in the top right corner
1. In the Camel tree, expand the `Routes` node, and select the first node, which is the `cbr-route` route. And click the *Diagram* button to see a visual representation of the route.
1. Notice the numbers in the diagram, which illustrate that 2 messages has been processed. 
1. You can click the *Log* button the navigation bar to see the business logging.


## Undeploy this example

The following information is divded into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the command line shell

To stop and undeploy the example in fabric8:

1. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild

### Using the web console

To stop and undeploy the example in fabric8:

1. In the web console, click the *Runtime* button in the navigation bar.
1. Select the `mychild` container in the *Containers* list, and click the *Stop* button in the top right corner


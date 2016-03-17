camel-amq: Demonstrates how to use the camel-amq component
======================================================
Author: Fuse Team  
Level: Beginner  
Technologies: Camel, ActiveMQ  
Summary: This quickstart demonstrates how to use the camel-amq component o connect to the local A-MQ broker and use JMS messaging between two Camel routes.  
Target Product: Fuse  
Source: <https://github.com/jboss-fuse/quickstarts>  

In this quickstart, orders from zoos all over the world will be copied from the input directory into a specific
output directory per country.

In this example we will use two containers, one container to run as a standalone A-MQ broker, and another as a client to the broker, where the Camel routes is running. This scenario is illustrated in the figure below:

![Camel AMQ Quickstart Diagram](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/camel-amq-quickstart-diagram.jpg)

The two Camel routes send and receives JMS message using the `amq:incomingOrders` endpoint, which is a queue on the A-MQ broker.

## Building this example

### Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To try the example you do not need to build from source first. Although building from source allows you to modify the source code, and re-deploy the changes to fabric. See more details on the fabric8 website about the [developer workflow](http://fabric8.io/gitbook/developer.html).

To build from the source code:

1. Change your working directory to `quickstarts/camel-amq` directory.
1. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `quickstarts/camel-amq` directory.
1. Run `mvn fabric8:deploy` to upload the quickstart to the fabric container.

If you run the `fabric:deploy` command for the first then, it will ask you for the username and password to login the fabric container.
And then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).

## How to run this example

The following information is divded into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the command line shell

This example requires using an A-MQ broker first.

#### Installing the A-MQ Broker

You can deploy a new A-MQ broker from the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new A-MQ standalone broker profile using `mygroup` as group name, and named mybroker 

        mq-create --kind StandAlone --group mygroup mybroker

1. Create a child container as the A-MQ broker using the mybroker profile.

        container-create-child --profile mq-broker-mygroup.mybroker root mybroker

#### Installing the quickstart

After installing the A-MQ broker we can install the example from the console command line, as follows:

1. Create a new child container and deploy the `quickstarts-camel.amq` profile in a single step, by entering the
 following command at the console. Notice we have add `--profile mq-client-mygroup` so the client connects to correct A-MQ group; as you can have multiple broker groups in fabric8.

        fabric:container-create-child --profile quickstarts-camel.amq --profile mq-client-mygroup root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.

### Using the web console

This example requires using an A-MQ broker first.

#### Installing the A-MQ Broker

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Login the web console
1. Click the Runtime button tin the navigation bar, and then click the MQ button in the sub navigation bar.
1. Click the Create broker configuration button
1. Select StandAlone as kind, and enter `mygroup` as group, and `mybroker` as broker name.
1. Click the Create broker button in the top right corner, and wait until the screen changes to show the A-MQ groups, as shown in the figure below:

![MQ Broker Groups](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/mq-broker-groups.png)

1. To create a new container using this broker profile, you can click the red triangle button (alert icon) which takes you to the Create New Container page, having pre-selected the broker profile.
1. Enter `mybroker` as the container name, and click the *Create and start container* button

#### Installing the quickstart

After installing the A-MQ broker we can install the example from the web console, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Login the web console
1. Click the Wiki button in the navigation bar
1. Select `quickstarts` --> `camel.amq`
1. Click the `New` button in the top right corner
1. In the Create New Container page, in the profiles filter field enter `my` and select the `mq-client-mygroup` from the list, as shown in the figure below. We do this so the client connects to correct A-MQ group; as you can have multiple broker groups in fabric8.

![MQ Client Create Wizard](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/mq-client-create-wizard.jpg)

1. And in the Container Name field enter `mychild` and click the *Create and start container* button


## How to try this example

The following information is divded into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the command line shell

To use the application be sure to have deployed the quickstart in fabric8 as described above. Successful deployment will create and start a Camel route in fabric8.

1. As soon as the Camel route has been started, you will see a directory `instances/mychild/work/jms/input` in your fabric8 installation.
1. Copy the files you find in this quickstart's `src/main/fabric8/data` directory to the newly created `instances/mychild/work/jms/input` directory.
1. Wait a few moments and you will find the same files organized by country under the `instances/mychild/work/jms/output` directory.
  * `order1.xml` in `work/jms/output/others`
  * `order2.xml` and `order4.xml` in `work/jms/output/uk`
  * `order3.xml` and `order5.xml` in `work/jms/output/us`

1. Use `log:display` to check out the business logging.
        Receiving order order1.xml
        Sending order order1.xml to another country
        Done processing order1.xml

### Using the web console

1. Login the web console
1. Click the Runtime button in the navigation bar
1. Select the `mychild` container in the containers list, and click the *open* button right next to the container name.
1. A new window opens and connects to the container. Click the *Camel* button in the navigation bar.
1. In the Camel tree, expand the `Endpoints` tree, and select the second node, which is `file://work/jms/input`, and click the *Send* button in the sub navigation bar.
1. Click the *Choose* button and mark [x] for the five `data/order1.xml` ... `data/order5.xml` files, as shown in the figure below.

        ![Camel Endpoint Choose](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/camel-endpoint-choose.jpg)

1. Click the *Send 5 files* button in the top right corner
1. In the Camel tree, expand the `Routes` node, and select the second node, which is the `jms-cbr-route` route. And click the *Diagram* button to see a visual representation of the route.
1. Notice the numbers in the diagram, which illustrate that 5 messages has been processed, of which 2 were from UK, 2 from US, and 1 others. 
1. You can click the *Log* button the navigation bar to see the business logging.


## Undeploy this example

The following information is divded into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the command line shell

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
1. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild

1. Stop and delete the A-MQ container by entering the following command at the console:

        fabric:container-stop mybroker
        fabric:container-delete mybroker

### Using the web console

To stop and undeploy the example in fabric8:

1. In the web console, click the *Runtime* button in the navigation bar.
1. Select the `mychild` and `mybroker` containers in the *Containers* list, and click the *Stop* button in the top right corner

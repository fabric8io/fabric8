jms: demonstrates how to connect to the local ActiveMQ broker and use JMS messaging between two Camel routes
===================================

## What is it?

This quickstart demonstrates how to connect to the local ActiveMQ broker and use JMS messaging between two Camel routes.

In this quickstart, orders from zoos all over the world will be copied from the input directory into a specific
output directory per country.


## System requirements

Before building and running this quick start you need:

* Maven 3.0.4 or higher
* JDK 1.7
* Fabric8


## Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To build from the source code:

1. Change your working directory to `jms` directory.
2. Run `mvn clean install` to build the quickstart.


## How to run this example

This example requires using an A-MQ broker first.

### Installing the A-MQ Broker

You can deploy a new A-MQ broker from the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new A-MQ standalone broker profile using `mygroup` as group name, and named mybroker 

     mq-create --kind StandAlone --group mygroup mybroker

1. Create a child container as the A-MQ broker using the mybroker profile.

    container-create-child --profile mq-broker-mygroup.mybroker root mybroker

### Installing and running the client

After installing the A-MQ broker we can install the example from the console command line, as follows:

1. Create a new child container and deploy the `example-quickstarts-jms` profile in a single step, by entering the
 following command at the console. Notice we have add `--profile mq-client-mygroup` so the client connects to correct A-MQ group; as you can have multiple broker groups in fabric8.

        fabric:container-create-child --profile example-quickstarts-jms --profile mq-client-mygroup root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.
1. Log into the `mychild` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect mychild

1. View the container log using the `log:tail` command as follows:

        log:tail


### How to try this example

To use the application be sure to have deployed the quickstart in fabric8 as described above. Successful deployment will create and start a Camel route in fabric8.

1. As soon as the Camel route has been started, you will see a directory `instances/mychild/work/jms/input` in your fabric8 installation.
2. Copy the files you find in this quickstart's `src/main/resources/data` directory to the newly created `instances/mychild/work/jms/input` directory.
3. Wait a few moments and you will find the same files organized by country under the `instances/mychild/work/jms/output` directory.
  * `order1.xml` in `work/jms/output/others`
  * `order2.xml` and `order4.xml` in `work/jms/output/uk`
  * `order3.xml` and `order5.xml` in `work/jms/output/us`


4. Use `log:display` to check out the business logging.
        Receiving order order1.xml
        Sending order order1.xml to another country
        Done processing order1.xml


## Undeploy this example

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
2. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild

3. Stop and delete the A-MQ container by entering the following command at the console:

        fabric:container-stop mybroker
        fabric:container-delete mybroker



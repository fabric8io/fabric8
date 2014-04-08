eip: demonstrates how to combine multiple Enterprise Integration Patterns to solve integration problems
===================================

What is it?
-----------

This quickstart demonstrates how to combine multiple EIPs to solve integration problems.

In this example, an orders file containing several orders for zoos around the world is sent to us.

We first want to make sure we retain a copy of the original file. This is done using the Wiretap EIP.

After saving the original, we want to split the file up into the individual orders. This is done using the Splitter EIP.

Then we want to store the orders in separate directories by geographical region. This is done using a Recipient List EIP.

Finally, we want to filter out the orders that contain more than 100 animals and generate a message for the strategic account team. This is done using a Filter EIP.

In studying this example you will learn:

* how to define a Camel route using the Blueprint XML syntax
* how to build and deploy an OSGi bundle in Fabric8
* how to combine multiple Enterprise Integration Patterns to create an integration solution
* how to use the Wiretap EIP to copy messages as they pass through a route
* how to use the Splitter EIP to split large messages into smaller ones
* how to use a Recipient List EIP to dynamically determine how a message passes through a route
* how to use the Filter EIP to filter messages and execute logic for the ones that match the filter
* how to define and use a bean to process a message
* how to use a `direct:` endpoint to link multiple smaller routes together


For more information see:

* http://camel.apache.org/recipient-list.html
* http://camel.apache.org/wire-tap.html
* http://camel.apache.org/message-filter.html
* http://camel.apache.org/resequencer.html
* http://fabric8.io/#/site/book/doc/index.md for more information about using Fabric8


System requirements
-------------------

Before building and running this example you need:

* Maven 3.0.4 or higher
* JDK 1.6 or 1.7
* Fabric8


Build and Deploy the Quickstart
-------------------------------

1. Change your working directory to `eip` directory.
*. Run `mvn clean install` to build the quickstart.
*. Start Fabric8 by running bin/fabric8 (on Linux) or bin\fabric8.bat (on Windows).
*. In the Fabric8 console, enter the following command:

        osgi:install -s mvn:io.fabric8.quickstarts.fabric/eip/${project.version}

*. Fabric8 should give you an id when the bundle is deployed
*. You can check that everything is ok by issuing  the command:

        osgi:list
   your bundle should be present at the end of the list


Use the bundle
--------------

To use the application be sure to have deployed the quickstart in Fabric8 as described above. Successful deployment will create and start a Camel route in Fabric8.

1. As soon as the Camel route has been started, you will see a directory `work/eip/input` in your Fabric8 installation.
2. Copy the file you find in this example's `src/main/resources/data` directory to the newly created `work/eip/input`
directory.
3. Wait a few moments and you will find multiple files organized by geographical region under `work/eip/output':
** `2012_0003.xml` and `2012_0005.xml` in `work/eip/output/AMER`
** `2012_0020.xml` in `work/eip/output/APAC`
** `2012_0001.xml`, `2012_0002.xml` and `2012_0004.xml` in `work/eip/output/EMEA`
4. Use `log:display` on the ESB shell to check out the business logging.
        [main]    Processing orders.xml
        [wiretap]  Archiving orders.xml
        [splitter] Shipping order 2012_0001 to region EMEA
        [splitter] Shipping order 2012_0002 to region EMEA
        [filter]   Order 2012_0002 is an order for more than 100 animals
        ...

Undeploy the Bundle
-------------------

To stop and undeploy the bundle in Fabric8:

1. Enter `osgi:list` command to retrieve your bundle id
2. To stop and uninstall the bundle enter

        osgi:uninstall <id>
 


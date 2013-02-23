# Enterprise Integration Patterns

## Overview
This example demonstrates how to combine multiple EIPs to solve integration problems.

In this example, an orders file containing several orders for zoos around the world is sent to us.

We first want to make sure we retain a copy of the original file. This is done using the Wiretap EIP.

After saving the original, we want want to split the file up into the individual orders. This is done using the Splitter EIP.

Then we want to store the orders in separate directories by geographical region. This is done using a Recipient List EIP.

Finally, we want to filter out the orders that container more than 100 animals and generate a message for the strategic account team. This is done using a Filter EIP.

## What You Will Learn
In studying this example you will learn:

* how to define a Camel route using the Blueprint XML syntax
* how to build and deploy a Fuse Application Bundle (FAB) in JBoss Fuse
* how to combine multiple Enterprise Integration Patterns to create an integration solution
* how to use the Wiretap EIP to copy messages as they pass through a route
* how to use the Splitter EIP to split large messages into smaller ones
* how to use a Recipient List EIP to dynamically determine how a message passes through a route
* how to use the Filter EIP to filter messages and execute logic for the ones that match the filter
* how to define and use a bean to process a message
* how to use a `direct:` endpoint to link multiple smaller routes together

## Prerequisites
Before building and running this example you need:

* Maven 3.0.3 or higher
* JDK 1.6 or 1.7
* JBoss Fuse 6

## Files in the Example
* `pom.xml` - the Maven POM file for building the example
* `src/main/java/org.jboss.fuse.examples/eip/RegionSupport.java` - a Java class used to determine the region code used by the recipient list
* `src/main/resources/OSGI-INF/blueprint/eip.xml` - the OSGI Blueprint file that defines the routes
* `test/data/orders.xml` - the data file that can be used to test the route
* `test/java/RegionSupportTest.java` - a JUnit test class for `RegionSupport`

## Building the Example
To build the example:

1. Change your working directory to the `examples/eip` directory.
2. Run `mvn clean install` to build the example.

## Running the Example
To run the example:

1. Start JBoss Fuse 6 by running `bin/fuse` (on Linux) or `bin\fuse.bat` (on Windows).
2. In the JBoss Fuse console, enter the following command:
        osgi:install -s fab:mvn:org.jboss.fuse.examples/eip/${project.version}
3. As soon as the Camel route has been started, you will see a directory `work/eip/input` in your JBoss Fuse installation.
4. Copy the file you find in this example's `src/test/data` directory to the newly created `work/eip/input` directory.
5. Wait a few moment and you will find multiple files organized by geographical region under `work/eip/output':
** `2012_0003.xml` and `2012_0005.xml` in `work/eip/output/AMER`
** `2012_0020.xml` in `work/eip/output/APAC`
** `2012_0001.xml`, `2012_0002.xml` and `2012_0004.xml` in `work/eip/output/EMEA`
6. Use `log:display` on the ESB shell to check out the business logging.
        [main]    Processing orders.xml
        [wiretap]  Archiving orders.xml
        [splitter] Shipping order 2012_0001 to region EMEA
        [splitter] Shipping order 2012_0002 to region EMEA
        [filter]   Order 2012_0002 is an order for more than 100 animals
        ...

## More information
For more information see:
* http://fusesource.com/docs/esbent/7.0/camel_eip/MsgRout-RecipientList.html for the Recipient List EIP
* http://fusesource.com/docs/esbent/7.0/camel_eip/_IDU_WireTap.html for the Wire Tap EIP
* http://fusesource.com/docs/esbent/7.0/camel_eip/MsgRout-MsgFilter.html for the Message Filter EIP
* http://fusesource.com/docs/esbent/7.0/camel_eip/MsgRout-Splitter.html for the Splitter EIP
* http://fusesource.com/documentation/fuse-esb-enterprise-documentation for more information about using JBoss Fuse

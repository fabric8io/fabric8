# Content-Based Router

## Overview
This example demonstrates how to use the content-based router (CBR) pattern in Camel to send a message
to a different destination based on the contents of the message.

In this example, orders from zoos all over the world will be copied from the input directory into a specific
output directory per country.

## What You Will Learn
In studying this example you will learn:

* how to define a Camel route using the Blueprint XML syntax
* how to build and deploy a Fuse Application Bundle (FAB) in JBoss Fuse
* how to use the CBR enterprise integration pattern

## Prerequisites
Before building and running this example you need:

* Maven 3.0.3 or higher
* JDK 1.6 or 1.7
* JBoss Fuse 6

## Files in the Example
* `pom.xml` - the Maven POM file for building the example
* `src/main/resources/OSGI-INF/blueprint/cbr.xml` - the OSGI Blueprint file that defines the route
* `test/data/*.xml` - data files that can be used to test the route

## Building the Example
To build the example:

1. Change your working directory to the `examples/cbr` directory.
2. Run `mvn clean install` to build the example.

## Running the Example
To run the example:

1. Start JBoss Fuse 6 by running `bin/fuseesb` (on Linux) or `bin\fuseesb.bat` (on Windows).
2. In the JBoss Fuse console, enter the following command:
        osgi:install -s fab:mvn:org.fusesource.examples/cbr/${project.version}
3. As soon as the Camel route has been started, you will see a directory `work/cbr/input` in your JBoss Fuse installation.
4. Copy the files you find in this example's `src/test/data` directory to the newly created `work/cbr/input` directory.
5. Wait a few moment and you will find the same files organized by country under the `work/cbr/output` directory.
** `order1.xml` in `work/cbr/output/others`
** `order2.xml` and `order4.xml` in `work/cbr/output/uk`
** `order3.xml` and `order5.xml` in `work/cbr/output/us`
6. Use `log:display` to check out the business logging.
        Receiving order order1.xml
        Sending order order1.xml to another country
        Done processing order1.xml

## More information
For more information see:

* http://www.enterpriseintegrationpatterns.com/ContentBasedRouter.html for more information about the CBR EIP
* http://fusesource.com/documentation/fuse-esb-enterprise-documentation for more information about using JBoss Fuse

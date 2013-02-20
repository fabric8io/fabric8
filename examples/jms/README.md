# JMS Based Router

## Overview
This example demonstrates how to connect to the local ActiveMQ broker and use JMS messaging between two Camel routes.

In this example, orders from zoos all over the world will be copied from the input directory into a specific
output directory per country.

## What You Will Learn
In studying this example you will learn:

* how to connect to the local ActiveMQ broker
* how to define a Camel route using the Blueprint XML syntax
* how to build and deploy a Fuse Application Bundle (FAB) in JBoss Fuse
* how to use the CBR enterprise integration pattern

## Prerequisites
Before building and running this example you need:

* Maven 3.0.3 or higher
* JDK 1.6 or 1.7
* JBoss Fuse 6 (medium or full distribution)

## Files in the Example
* `pom.xml` - the Maven POM file for building the example
* `src/main/resources/OSGI-INF/blueprint/camel-context.xml` - the OSGI Blueprint file that defines the route
* `test/data/*.xml` - data files that can be used to test the route

## Building the Example
To build the example:

1. Verify etc/users.properties from the JBoss Fuse installation contains the following 'admin' user configured:

admin=admin,admin

If some other user is configured you will need to modify the 'activemq' bean in src/main/resources/OSGI-INF/blueprint/camel-context.xml
to use the user defined in etc/users.properties.

2. Change your working directory to the `examples/jms` directory.
3. Run `mvn clean install` to build the example.

## Running the Example
To run the example:

1. Start JBoss Fuse 6 by running `bin/fuse` (on Linux) or `bin\fuse.bat` (on Windows).
2. In the JBoss Fuse console, enter the following command:
        osgi:install -s fab:mvn:org.fusesource.examples/jms/${project.version}
3. As soon as the Camel route has been started, you will see a directory `work/jms/input` in your JBoss Fuse installation.
4. Copy the files you find in this example's `src/test/data` directory to the newly created `work/jms/input` directory.
5. Wait a few moments and you will find the same files organized by country under the `work/jms/output` directory.
** `order1.xml` in `work/jms/output/others`
** `order2.xml` and `order4.xml` in `work/jms/output/uk`
** `order3.xml` and `order5.xml` in `work/jms/output/us`
6. Use `log:display` to check out the business logging.
        Receiving order order1.xml
        Sending order order1.xml to another country
        Done processing order1.xml

## More information
For more information see:

* http://www.enterpriseintegrationpatterns.com/ContentBasedRouter.html for more information about the CBR EIP
* http://fusesource.com/documentation/fuse-esb-enterprise-documentation for more information about using JBoss Fuse

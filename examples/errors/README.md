# Exception Handling

## Overview
This example demonstrates how to handle exceptions that occur while routing messages with Camel.

This example show you how to add a default error handler to your Camel context for all uncaught exceptions. Additionally, it will show you how to add exception handling routines for dealing with specific exception types.

## What You Will Learn
In studying this example you will learn:

* how to define a Camel route using the Blueprint XML syntax
* how to build and deploy a Fuse Application Bundle (FAB) in JBoss Fuse
* how to define a default error handler to your Camel context
* how to define exception-specific error handling routines

## Prerequisites
Before building and running this example you need:

* Maven 3.0.3 or higher
* JDK 1.6 or 1.7
* JBoss Fuse 6

## Files in the Example
* `pom.xml` - the Maven POM file for building the example
* `src/main/java/org.jboss.fuse.examples/errors/OrderService.java` - a Java class used to validate the orders being processed by the route. It generates exceptions when an order is placed on a Sunday and also at random intervals.
* `src/main/java/org.jboss.fuse.examples/errors/OrderValidationException.java` - a Java class defining the exception thrown when an order is invalid
* `src/main/resources/OSGI-INF/blueprint/errors.xml` - the OSGI Blueprint file that defines the route
* `test/data/*.xml` - data files that can be used to test the example
* `src/test/java/org.jboss.fuse.examples/errors/OrderServiceTest.java` - a JUnit test class
* `src/test/resources/log4j.properties` - configuration for formatting the test output

## Building the Example
To build the example:

1. Change your working directory to the `examples/errors` directory.
2. Run `mvn clean install` to build the example.

## Running the Example
To run the example:

1. Start JBoss Fuse 6 by running bin/fuse (on Linux) or bin\fuse.bat (on Windows).
2. In the JBoss Fuse console, enter the following command:
        osgi:install -s fab:mvn:org.jboss.fuse.examples/errors/${project.version}
3. As soon as the Camel route has been started, you will see a directory `work/errors/input` in your JBoss Fuse installation.
4. Copy the file you find in this example's `src/test/data` directory to the newly created `work/errors/input` directory.
5. Wait a few moment and you will find the files in directories under `work/errors`:
** `order4.xml` will always end up in the `work/errors/validation` directory
** other files will end up in `work/errors/done` or `work/errors/deadletter` depending on the runtime exceptions that occur
6. Use `log:display` to check out the business logging - the exact output may look differently because the 'unexpected runtime exception...' happen randomly
        Processing order4.xml
        Order validation failure: order date 2012-03-04 should not be a Sunday
        Validation failed for order4.xml - moving the file to work/errors/validation
        Processing order5.xml
        An unexcepted runtime exception occurred while processing order5.xml
        Done processing order5.xml
        ...

## More information
For more information see:

* http://www.enterpriseintegrationpatterns.com/DeadLetterChannel.html for the Dead Letter Channel EIP
* https://access.redhat.com/knowledge/docs/JBoss_Fuse for more information about using JBoss Fuse

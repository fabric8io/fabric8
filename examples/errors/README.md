# Content-Based Router

## Overview
This example demonstrates how to handle exceptions that occur while routing messages with Camel.

We will show you how to add a default error handler to your Camel context for all uncaught exceptions but we will
also show you how you can add additional exception handling routines for dealing with specific exception types.

## What You Will Learn
In studying this example you will learn:

* how to define a Camel route using the Blueprint XML syntax
* how to build and deploy a Fuse Application Bundle (FAB) in Fuse ESB
* how to define a default error handler to your Camel context
* how to define exception-specific error handling routines

## Prerequisites
Before building and running this example you need:

* Maven 3.0.3 or higher
* JDK 1.6
* Fuse ESB Enterprise 7

## Building the Example
To build the example:

1. Change your working directory to the examples/errors directory
2. Run `mvn clean install` to build the example

## Running the Example
To run the example:

1. Start Fuse ESB Enterprise 7 by running bin/fuseesb (on Linux) or bin\fuseesb.bat (on Windows)
2. In the Fuse ESB console, enter the following command: `osgi:install -s fab:mvn:org.fusesource.examples/errors/${project.version}`
3. As soon as the Camel route has been started, you will see a directory work/errors/input in your Fuse ESB installation
4. Copy the file you find in this example's src/test/data directory to the newly created work/errors/input directory
5. Wait a few moment and you will find the files in other folder under work/errors
        order4.xml will always end up in the work/errors/validation folder
        other files will end up in work/errors/output or work/errors/deadletter depending on the runtime exceptions that occur
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

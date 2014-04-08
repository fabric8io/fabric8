errors: demonstrates exception handling in Camel
===================================

What is it?
-----------

This quickstart demonstrates how to handle exceptions that occur while routing messages with Camel.

This quickstart show you how to add a default error handler to your Camel context for all uncaught exceptions.
Additionally, it will show you how to add exception handling routines for dealing with specific exception types.

In studying this quick start you will learn:

* how to define a Camel route using the Blueprint XML syntax
* how to build and deploy an OSGi bundle in Fabric8
* how to define a default error handler to your Camel context
* how to define exception-specific error handling routines

For more information see:

* http://camel.apache.org/dead-letter-channel.html
* http://fabric8.io/#/site/book/doc/index.md for more information about using Fabric8


System requirements
-------------------

Before building and running this quick start you need:

* Maven 3.0.4 or higher
* JDK 1.6 or 1.7
* Fabric8


Build and Deploy the Quickstart
-------------------------------

1. Change your working directory to `errors` directory.
*. Run `mvn clean install` to build the quickstart.
*. Start Fabric8 by running bin/fabric8 (on Linux) or bin\fabric8.bat (on Windows).
*. In the Fabric8 console, enter the following command:

        osgi:install -s mvn:io.fabric8.quickstarts.fabric/errors/${project.version}

*. Fabric8 should give you an id when the bundle is deployed
*. You can check that everything is ok by issuing  the command:

        osgi:list
   your bundle should be present at the end of the list


Use the bundle
--------------

To use the application be sure to have deployed the quickstart in Fabric8 as described above. Successful deployment will create and start a Camel route in Fabric8.

1. As soon as the Camel route has been started, you will see a directory `work/errors/input` in your Fabric8 installation.
2. Copy the file you find in this quick start's `src/main/resources/data` directory to the newly created
`work/errors/input` directory.
4. Wait a few moments and you will find the files in directories under `work/errors`:

  * `order4.xml` will always end up in the `work/errors/validation` directory
  * other files will end up in `work/errors/done` or `work/errors/deadletter` depending on the runtime exceptions that occur
5. Use `log:display` to check out the business logging - the exact output may look differently because the 'unexpected runtime exception...' happen randomly

        Processing order4.xml
        Order validation failure: order date 2012-03-04 should not be a Sunday
        Validation failed for order4.xml - moving the file to work/errors/validation
        Processing order5.xml
        An unexcepted runtime exception occurred while processing order5.xml
        Done processing order5.xml
        ...

Undeploy the Bundle
-------------------

To stop and undeploy the bundle in Fabric8:

1. Enter `osgi:list` command to retrieve your bundle id
2. To stop and uninstall the bundle enter

        osgi:uninstall <id>
 

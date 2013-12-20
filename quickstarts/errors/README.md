errors: demonstrates exception handling in Camel
===================================
Author: Fuse Team  
Level: Beginner  
Technologies: Fuse, OSGi, Camel  
Summary: Demonstrates Exception handling in Camel  
Target Product: Fuse  
Source: <https://github.com/jboss-fuse/quickstarts>

What is it?
-----------

This quickstart demonstrates how to handle exceptions that occur while routing messages with Camel.

This quickstart show you how to add a default error handler to your Camel context for all uncaught exceptions.
Additionally, it will show you how to add exception handling routines for dealing with specific exception types.

In studying this quick start you will learn:

* how to define a Camel route using the Blueprint XML syntax
* how to build and deploy an OSGi bundle in JBoss Fuse
* how to define a default error handler to your Camel context
* how to define exception-specific error handling routines

For more information see:

* http://www.enterpriseintegrationpatterns.com/DeadLetterChannel.html for the Dead Letter Channel EIP
* https://access.redhat.com/site/documentation/JBoss_Fuse/ for more information about using JBoss Fuse


System requirements
-------------------

Before building and running this quick start you need:

* Maven 3.0.4 or higher
* JDK 1.6 or 1.7
* JBoss Fuse 6


Build and Deploy the Quickstart
-------------------------------

1. Change your working directory to `errors` directory.
*. Run `mvn clean install` to build the quickstart.
*. Start JBoss Fuse 6 by running bin/fuse (on Linux) or bin\fuse.bat (on Windows).
*. In the JBoss Fuse console, enter the following command:

        osgi:install -s mvn:org.jboss.quickstarts.fuse/errors/${project.version}

*. Fuse should give you an id when the bundle is deployed
*. You can check that everything is ok by issuing  the command:

        osgi:list
   your bundle should be present at the end of the list


Use the bundle
--------------

To use the application be sure to have deployed the quickstart in Fuse as described above. Successful deployment will create and start a Camel route in Fuse.

1. As soon as the Camel route has been started, you will see a directory `work/errors/input` in your JBoss Fuse installation.
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

To stop and undeploy the bundle in Fuse:

1. Enter `osgi:list` command to retrieve your bundle id
2. To stop and uninstall the bundle enter

        osgi:uninstall <id>
 

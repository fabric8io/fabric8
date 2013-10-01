jms: demonstrates how to connect to the local ActiveMQ broker and use JMS messaging between two Camel routes
===================================
Author: Fuse Team  
Level: Beginner  
Technologies: Fuse, ActiveMQ, Blueprint, Camel  
Summary: Demonstrates how to connect to the local ActiveMQ broker and use JMS messaging between two Camel routes  
Target Product: Fuse  
Source: <https://github.com/jboss-fuse/quickstarts>

What is it?
-----------

This quickstart demonstrates how to connect to the local ActiveMQ broker and use JMS messaging between two Camel routes.

In this quickstart, orders from zoos all over the world will be copied from the input directory into a specific
output directory per country.

In studying this quick start you will learn:

* how to connect to the local ActiveMQ broker
* how to define a Camel route using the Blueprint XML syntax
* how to build and deploy a Fuse Application Bundle (FAB) in JBoss Fuse
* how to use the CBR enterprise integration pattern

For more information see:

* http://www.enterpriseintegrationpatterns.com/ContentBasedRouter.html for more information about the CBR EIP
* https://access.redhat.com/knowledge/docs/JBoss_Fuse/ for more information about using JBoss Fuse


System requirements
-------------------

Before building and running this quick start you need:

* Maven 3.0.3 or higher
* JDK 1.6 or 1.7
* JBoss Fuse 6 (medium or full distribution)


Build and Deploy the Quickstart
-------------------------

1. Make sure you have once launched the build from `quickstarts` root by running `mvn clean install` in `quickstarts` folder to install quickstart bom in your local repository
2. Verify etc/users.properties from the JBoss Fuse installation contains the following 'admin' user configured:

        admin=admin,admin

    If some other user is configured you will need to modify the 'activemq' bean in src/main/resources/OSGI-INF/blueprint/camel-context.xml to use the user defined in etc/users.properties.

3. Change your working directory to `quckstarts/jms` directory.
* Run `mvn clean install` to build the quickstart.
* Start JBoss Fuse 6 by running bin/fuse (on Linux) or bin\fuse.bat (on Windows).
* In the JBoss Fuse console, enter the following command:

        osgi:install -s fab:mvn:org.jboss.quickstarts.fuse/jms/<project version>

* Fuse should give you on id when the bundle is deployed
* You can check that everything is ok by issue the command:

        osgi:list
   your bundle should be present at the end of the list


Use the bundle
-------------------

To use the application be sure to have deployed the quickstart in Fuse as described above. Successful deployment will create and start a Camel route in Fuse.

1. As soon as the Camel route has been started, you will see a directory `work/jms/input` in your JBoss Fuse installation.
2. Copy the files you find in this quickstart's `src/main/resources/data` directory to the newly created `work/jms/input` directory.
3. Wait a few moments and you will find the same files organized by country under the `work/jms/output` directory.
  * `order1.xml` in `work/jms/output/others`
  * `order2.xml` and `order4.xml` in `work/jms/output/uk`
  * `order3.xml` and `order5.xml` in `work/jms/output/us`


4. Use `log:display` to check out the business logging.
        Receiving order order1.xml
        Sending order order1.xml to another country
        Done processing order1.xml

Undeploy the Bundle
--------------------

To stop and undeploy the bundle in Fuse:

1. Enter `osgi:list` command to retrieve your bundle id
2. To stop and uninstall the bundle enter

        osgi:uninstall <id>
 

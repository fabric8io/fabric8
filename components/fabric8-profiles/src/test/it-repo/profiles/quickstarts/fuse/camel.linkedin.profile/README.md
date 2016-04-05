camel-linkedin: Demonstrates the camel-linkedin component
======================================================
Author: Fuse Team  
Level: Beginner  
Technologies: Camel, Blueprint  
Summary: This quickstart demonstrates how to use the camel-linkedin component in Camel to poll LinkedIn user connections  
Target Product: Fuse  
Source: <https://github.com/jboss-fuse/quickstarts>  



What is it?
-----------

This quick start shows how to use Apache Camel, and its OSGi integration to use the OSGi config admin and poll connections from LinkedIn.

This quick start combines use of the Camel LinkedIn component to poll user's connections, and write them to a simple text file.

In studying this quick start you will learn:

* how to define a Camel route using the Blueprint XML syntax
* how to build and deploy an OSGi bundle in JBoss Fuse
* how to use OSGi config admin in JBoss Fuse
* how to use the Camel LinkedIn component

For more information see:

* https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_Fuse/6.2/html/Apache_Camel_Component_Reference/files/_IDU_LinkedIn.html for more information about the Camel LinkedIn component
* https://access.redhat.com/site/documentation/JBoss_Fuse/ for more information about using JBoss Fuse

System requirements
-------------------

Before building and running this quick start you need:

* Maven 3.1.1 or higher
* JDK 1.7 or 1.8
* JBoss Fuse 6

Build and Deploy the Quickstart
-------------------------

1. Change your working directory to `camel-linkedin` directory.
* Run `mvn clean install` to build the quickstart.
* Start JBoss Fuse 6 by running bin/fuse (on Linux) or bin\fuse.bat (on Windows).
* Create the following configuration file in the etc/ directory of your Red Hat JBoss Fuse installation:

  InstallDir/etc/org.jboss.quickstarts.fuse.camel.linkedin.cfg
  Edit the org.jboss.quickstarts.fuse.camel.linkedin.cfg file with a text editor and add the following contents:

  userName=<LinkedIn account user name>
  userPassword=<LinkedIn account password>
  clientId=<LinkedIn client id>
  clientSecret=<LinkedIn client secret>

* In the JBoss Fuse console, enter the following commands:

        features:install camel-linkedin
        osgi:install -s mvn:org.jboss.quickstarts.fuse/camel-linkedin/${project.version}

* Fuse should give you an id when the bundle is deployed

* You can check that everything is ok by issuing  the command:

        osgi:list
   your bundle should be present at the end of the list


Use the bundle
---------------------

To use the application be sure to have deployed the quickstart in Fuse as described above. 

1. As soon as the Camel route has been started, you will see a directory `work/camel-linkedin/output` in your JBoss Fuse installation.
2. Wait a few moments and you will see the LinkedIn user's connections downloaded to your the connections.txt file.
The route will continue polling LinkedIn every 15 minutes and overwrite the output file.
3. Use `log:display` to check out the business logging.
        Poll received <n> user connections
        Writing connections to connections.txt
        Done downloading user connections

Undeploy the Archive
--------------------

To stop and undeploy the bundle in Fuse:

1. Enter `osgi:list` command to retrieve your bundle id
2. To stop and uninstall the bundle enter

        osgi:uninstall <id>

camel-box: Demonstrates how to use the camel-box component
======================================================
Author: Fuse Team  
Level: Beginner  
Technologies: Camel, Blueprint  
Summary: This quickstart demonstrates how to use the camel-box component in Camel in order to upload files to Box.com  
Target Product: Fuse  
Source: <https://github.com/jboss-fuse/quickstarts>  



What is it?
-----------

This quick start shows how to use Apache Camel, and its OSGi integration to use the OSGi config admin and upload files to Box.com.

This quick start combines use of the Camel File component, which reads files and uses the Camel Box component to upload them to a Box.com account's root folder.

In studying this quick start you will learn:

* how to define a Camel route using the Blueprint XML syntax
* how to build and deploy an OSGi bundle in JBoss Fuse
* how to use OSGi config admin in JBoss Fuse
* how to use the Camel Box component

For more information see:

* https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_Fuse/6.2/html/Apache_Camel_Component_Reference/files/_IDU_Box.html for more information about the Camel Box component
* https://access.redhat.com/site/documentation/JBoss_Fuse/ for more information about using JBoss Fuse

System requirements
-------------------

Before building and running this quick start you need:

* Maven 3.1.1 or higher
* JDK 1.7 or 1.8
* JBoss Fuse 6

Build and Deploy the Quickstart
-------------------------

1. Change your working directory to `camel-box` directory.
* Run `mvn clean install` to build the quickstart.
* Start JBoss Fuse 6 by running bin/fuse (on Linux) or bin\fuse.bat (on Windows).
* Create the following configuration file in the etc/ directory of your Red Hat JBoss Fuse installation:

  InstallDir/etc/org.jboss.quickstarts.fuse.camel.box.cfg
  Edit the org.jboss.quickstarts.fuse.camel.box.cfg file with a text editor and add the following contents:

  userName=<Box.com account user name>
  userPassword=<Box.com account password>
  clientId=<Box.com client id>
  clientSecret=<Box.com client secret>

* In the JBoss Fuse console, enter the following commands:

        features:install camel-box
        osgi:install -s mvn:org.jboss.quickstarts.fuse/camel-box/${project.version}

* Fuse should give you an id when the bundle is deployed

* You can check that everything is ok by issuing  the command:

        osgi:list
   your bundle should be present at the end of the list


Use the bundle
---------------------

To use the application be sure to have deployed the quickstart in Fuse as described above. 

1. As soon as the Camel route has been started, you will see a directory `work/camel-box/input` in your JBoss Fuse installation.
2. Copy the files you find in this quick start's `src/main/resources/data` directory to the newly created `work/camel-box/input`
directory.
3. Wait a few moments and you will find the same files uploaded to your Box.com root folder
4. Use `log:display` to check out the business logging.
        Receiving file test-camel-box.txt
        Sending file test-camel-box.txt to Box.com
        Done uploading test-camel-box.txt
5. Before running the example again, ensure that the files are deleted from the Box.com account

Undeploy the Archive
--------------------

To stop and undeploy the bundle in Fuse:

1. Enter `osgi:list` command to retrieve your bundle id
2. To stop and uninstall the bundle enter

        osgi:uninstall <id>

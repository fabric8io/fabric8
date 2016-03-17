camel-salesforce: Demonstrates the camel-salesforce component
======================================================
Author: Fuse Team  
Level: Beginner  
Technologies: Camel, Blueprint, JBoss Data Virtualization  
Summary: This quickstart demonstrates how to use the camel-salesforce component in Camel to integrate with Salesforce  
Target Product: Fuse  
Source: <https://github.com/jboss-fuse/quickstarts>  



What is it?
-----------

This quick start shows how to use Apache Camel, and its OSGi integration to use the OSGi config admin and create/update records in Salesforce.

This quick start combines use of the Camel JSON data format to read records from json files, and Camel Salesforce component to create/update records in Salesforce.
It also uses Salesforce streaming API to receive notifications for record updates.

In studying this quick start you will learn:

* how to define a Camel route using the Blueprint XML syntax
* how to build and deploy an OSGi bundle in JBoss Fuse
* how to use JSON data format in JBoss Fuse
* how to use OSGi config admin in JBoss Fuse
* how to use the Camel Salesforce component

For more information see:

* https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_Fuse/6.2/html/Apache_Camel_Component_Reference/files/_IDU_Salesforce.html for more information about the Camel Salesforce component
* https://access.redhat.com/site/documentation/JBoss_Fuse/ for more information about using JBoss Fuse

System requirements
-------------------

Before building and running this quick start you need:

* Maven 3.1.1 or higher
* JDK 1.7 or 1.8
* JBoss Fuse 6

Build and Deploy the Quickstart
-------------------------

* Create Cheese SObject in Salesforce with the following fields:

  Standard Fields:

    Name            Text(80)

  Custom Fields:

    Country         Text(80)
    Description     Long Text Area(1024)
    Milk            MultiSelect PickList with values Cow, Ewe, Goat

* Change your working directory to `camel-salesforce` directory.
* Edit the org.jboss.quickstarts.fuse.salesforce.cfg file in 'camel-salesforce' directory with a text editor and add the following contents:

  clientId=Salesforce app consumer key
  clientSecret=Salesforce app consumer secret
  userName=Salesforce user id
  password=Salesforce password with security token

* Run `mvn -Pgenerate-pojos clean install` to build the quickstart.
* Start JBoss Fuse 6 by running bin/fuse (on Linux) or bin\fuse.bat (on Windows).
* Copy the configuration file org.jboss.quickstarts.fuse.salesforce.cfg to the etc/ directory of your Red Hat JBoss Fuse installation:

  InstallDir/etc/org.jboss.quickstarts.fuse.salesforce.cfg

* In the JBoss Fuse console, enter the following commands:

        features:install camel-salesforce
        osgi:install -s mvn:org.jboss.quickstarts.fuse/camel-salesforce/${project.version}

* Fuse should give you an id when the bundle is deployed

* You can check that everything is ok by issuing  the command:

        osgi:list
   your bundle should be present at the end of the list


Use the bundle
---------------------

To use the application be sure to have deployed the quickstart in Fuse as described above. 

1. As soon as the Camel route has been started, you will see a directory `work/camel-salesforce/input` in your JBoss Fuse installation.
2. Copy the files you find in this quick start's `src/main/resources/data` directory to the newly created `work/camel-salesforce/input` directory.
3. Use `log:display` to check out the business logging.
        Receiving file cheese1.json
        Sending file cheese1.json to Salesforce
        Creating cheese with name Asiago...
        Receiving file cheese2.json
        Sending file cheese2.json to Salesforce
        Creating cheese with name Blue...
        Receiving file cheese3.json
        Sending file cheese3.json to Salesforce
        Creating cheese with name Gruyere...
        Created cheese Asiago with result success=true and errors=[]
        Created cheese Blue with result success=true and errors=[]
        Created cheese Gruyere with result success=true and errors=[]
4. In a few moments Salesforce should send streaming notifications, which are written to the directory 'work/camel-salesforce/output' in your JBoss Fuse installation.
5. Use `log:display` to check out the business logging for these notifications.
        Received created notification for Asiago
        Done writing notification to file Asiago.json
        Received created notification for Blue
        Done writing notification to file Blue.json
        Received created notification for Gruyere
        Done writing notification to file Gruyere.json4.


Undeploy the Archive
--------------------

To stop and undeploy the bundle in Fuse:

1. Enter `osgi:list` command to retrieve your bundle id
2. To stop and uninstall the bundle enter

        osgi:uninstall <id>

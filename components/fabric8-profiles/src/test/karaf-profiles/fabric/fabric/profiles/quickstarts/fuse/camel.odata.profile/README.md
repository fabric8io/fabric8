camel-odata: Demonstrates the camel-olingo2 component
======================================================
Author: Fuse Team  
Level: Beginner  
Technologies: Camel, Blueprint, JBoss Data Virtualization  
Summary: Demonstrates how to use the camel-olingo2 component in Camel to integrate with JBoss Data Virtualization (JDV) using OData 2.0  
Target Product: Fuse  
Source: <https://github.com/jboss-fuse/quickstarts>  



What is it?
-----------

This quick start shows how to use Apache Camel, and its OSGi integration to use the OSGi config admin and create records in JDV.

This quick start combines use of the Camel JSON data format to read records from json files, and Camel Olingo2 component to create records in JDV.

In studying this quick start you will learn:

* how to define a Camel route using the Blueprint XML syntax
* how to build and deploy an OSGi bundle in JBoss Fuse
* how to use JSON data format in JBoss Fuse
* how to use OSGi config admin in JBoss Fuse
* how to use the Camel Olingo2 component for OData

For more information see:

* https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_Fuse/6.2/html/Apache_Camel_Component_Reference/files/_IDU_Olingo2.html for more information about the Camel Olingo2 component
* https://access.redhat.com/site/documentation/JBoss_Fuse/ for more information about using JBoss Fuse

System requirements
-------------------

Before building and running this quick start you need:

* Maven 3.1.1 or higher
* JDK 1.7 or 1.8
* JBoss Fuse 6

Build and Deploy the Quickstart
-------------------------

* Create BOOKS OData service in JDV server by following the instructions at https://developer.jboss.org/wiki/ProducingAndConsumingODataInTeiidAndTeiidDesigner
  Note that if you are not using MySQL, change the <to/> uri in src/main/resources/OSGI-INF/blueprint/odata.xml to point to the correct OData service.
* Change your working directory to `camel-odata` directory.
* Run `mvn clean install` to build the quickstart.
* Start JBoss Fuse 6 by running bin/fuse (on Linux) or bin\fuse.bat (on Windows).
* Create the following configuration file in the etc/ directory of your Red Hat JBoss Fuse installation:

  InstallDir/etc/org.jboss.quickstarts.fuse.camel-odata.cfg
  Edit the org.jboss.quickstarts.fuse.camel-odata.cfg file with a text editor and add the following contents:

  userPassword=Basic <base64 encoded JBoss Data Virtualization password>
  serviceUri=http://localhost:8080/camel-odata/BooksRest
  contentType=application/atom+xml;charset=utf-8

* In the JBoss Fuse console, enter the following commands:

        features:install camel-olingo2
        features:install camel-jackson
        osgi:install -s mvn:org.jboss.quickstarts.fuse/camel-odata/${project.version}

* Fuse should give you an id when the bundle is deployed

* You can check that everything is ok by issuing  the command:

        osgi:list
   your bundle should be present at the end of the list


Use the bundle
---------------------

To use the application be sure to have deployed the quickstart in Fuse as described above. 

1. As soon as the Camel route has been started, you will see a directory `work/camel-odata/input` in your JBoss Fuse installation.
2. Copy the files you find in this quick start's `src/main/resources/data` directory to the newly created `work/camel-odata/input`
directory.
3. Use `log:display` to check out the business logging.
        Receiving file book1.json
        Sending file book1.json to JBoss Data Virtualization Server
        Done creating book with ISBN 0-042-123456
4. To re-run the example using the same sample data, delete the records created in the previous run

Undeploy the Archive
--------------------

To stop and undeploy the bundle in Fuse:

1. Enter `osgi:list` command to retrieve your bundle id
2. To stop and uninstall the bundle enter

        osgi:uninstall <id>

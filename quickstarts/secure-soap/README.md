secure-soap: demonstrates a secure SOAP web service with Apache CXF
==========================
Author: Fuse Team  
Level: Beginner  
Technologies: Fuse, OSGi, CXF, WS-Security  
Summary: Demonstrates a SOAP web service with Apache CXF  
Target Product: Fuse  
Source: <https://github.com/jboss-fuse/quickstarts>

What is it?
-----------
This quick start demonstrates how to create a Web service with Apache CXF using WS-Security and Blueprint configuration,
and expose it through the OSGi HTTP Service.

In studying this quick start you will learn:

* how to configure JAX-WS Web services by using the blueprint configuration file.
* how to configure WS-Security on a CXF JAX-WS Web service in Blueprint
* how to use standard Java Web Service annotations to define a Web service interface
* how to use standard Java Web Service annotations when implementing a Web service in Java
* how to use use an HTTP URL to invoke a remote Web service

For more information see:

* https://access.redhat.com/site/documentation/JBoss_Fuse/ for more information about using JBoss Fuse

System requirements
-------------------
Before building and running this quick start you need:

* Maven 3.0.4 or higher
* JDK 1.6 or 1.7
* JBoss Fuse 6


Build and Deploy the Quickstart
-------------------------------
To build the quick start:

1. Change your working directory to `secure-soap` directory.
* Run `mvn clean install` to build the quick start.
* Start JBoss Fuse 6 by running `bin/fuse` (on Linux) or `bin\fuse.bat` (on Windows).
* Verify etc/users.properties from the JBoss Fuse installation contains the following 'admin' user configured:
   admin=admin,admin
* In the JBoss Fuse console, enter the following command:

        osgi:install -s mvn:org.jboss.quickstarts.fuse/secure-soap/${project.version}

* Fuse should give you an id when the bundle is deployed
* You can check that everything is ok by issuing  the command:

        osgi:list
   your bundle should be present at the end of the list

Use the bundle
--------------
There are several ways you can interact with the running Web services:
* browse the Web service metadata
* access the service in a Web browser
* use a Java client

### Browsing Web service metadata

A full listing of all CXF Web services is available at

    http://localhost:8181/cxf

After you deployed this quick start, you will see the `HelloWorldSecurity` service appear in the `Available SOAP Services` section, together with a list of operations for the endpoint and some additional information like the endpoint's address and a link to the WSDL file for the Web service:

    http://localhost:8181/cxf/HelloWorldSecurity?wsdl


### To run the test:

In this cxf-jaxws quistart, we also provide an integration test which can perform a few HTTP requests to test our web services. We
created a Maven `test` profile to allow us to run tests code with a simple Maven command after having deployed the bundle to Fuse:

1. Change to the `secure-soap` directory.
2. Run the following command:

        mvn -Ptest

The test uses a client proxy for the Web service to invoke the remote method - in reality,
a SOAP message will be sent to the server and the response SOAP message will be received and handled.  You will see this output from the remote method:

        Apr 4, 2013 7:48:13 AM org.apache.cxf.service.factory.ReflectionServiceFactoryBean buildServiceFromClass
        INFO: Creating Service {http://secure.soap.fuse.quickstarts.jboss.org}HelloWorldService from class org.jboss.fuse.examples.cxf.jaxws.security.HelloWorld
        Hello World


### To run a Web client:

You can use an external tool such as SoapUI to test web services. 

When using SoapUI with WS Security, then configure the request properties as follows:

* Username = admin
* Password = admin
* Authentication Type = Global HTTP Settings
* WSS-Password Type = PasswordText


### Managing the user credentials

You can define additional users in the JAAS realm in two ways:

1. By editing the `etc/users.properties` file, adding a line for every user your want to add (syntax: `user = password, roles`).

            myuser = mysecretpassword

2. Using the jaas: commands in the JBoss Fuse console:

            jaas:manage --realm karaf --index 1
            jaas:useradd myuser mysecretpassword
            jaas:update


### Changing /cxf servlet alias

By default CXF Servlet is assigned a `/cxf` alias. You can change it in a couple of ways:

1. Add `org.apache.cxf.osgi.cfg` to the `/etc` directory and set the `org.apache.cxf.servlet.context` property, for example:

        org.apache.cxf.servlet.context=/custom

2. Use shell config commands, for example:

        config:edit org.apache.cxf.osgi
        config:propset org.apache.cxf.servlet.context /custom
        config:update

Undeploy the Bundle
-------------------

To stop and undeploy the bundle in Fuse:

1. Enter `osgi:list` command to retrieve your bundle id
2. To stop and uninstall the bundle enter

        osgi:uninstall <id>


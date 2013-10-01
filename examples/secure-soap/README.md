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

* https://access.redhat.com/knowledge/docs/JBoss_Fuse/ for more information about using JBoss Fuse

System requirements
-------------------
Before building and running this quick start you need:

* Maven 3.0.3 or higher
* JDK 1.6 or 1.7
* JBoss Fuse 6


Build and Deploy the Quickstart
-------------------------
To build the quick start:

1. Make sure you have once launched the build from `quickstarts` root by running `mvn clean install` in `quickstarts` folder to install quickstart bom in your local repository
* Change your working directory to `quickstarts/secure-soap` directory.
* Run `mvn clean install` to build the quick start.
* Start JBoss Fuse 6 by running `bin/fuse` (on Linux) or `bin\fuse.bat` (on Windows).
* Verify etc/users.properties from the JBoss Fuse installation contains the following 'admin' user configured:
   admin=admin,admin
* In the JBoss Fuse console, enter the following command:

        osgi:install -s fab:mvn:org.jboss.quickstarts.fuse/secure-soap/<project version>

* Fuse should give you on id when the bundle is deployed
* You can check that everything is ok by issue the command:

        osgi:list
   your bundle should be present at the end of the list

Use the bundle
----------------
There are several ways you can interact with the running Web services:
* browse the Web service metadata
* access the service in a Web browser
* use a Java client

### Browsing Web service metadata

A full listing of all CXF Web services is available at

    http://localhost:8181/cxf

After you deployed this quick start, you will see the `HelloWorldSecurity` service appear in the `Available SOAP Services` section, together with a list of operations for the endpoint and some additional information like the endpoint's address and a link to the WSDL file for the Web service:

    http://localhost:8181/cxf/HelloWorldSecurity?wsdl


### To run a Web client:

1. Open the `client.html`, which is located in the same directory as this README file, in your favorite browser.
2. Click the **Send** button to send a request.

   Once the request has been successfully sent, a response similar to the following should appear in the right-hand panel of the web page:

         STATUS: 200

         <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
           <soap:Body><ns2:sayHiResponse xmlns:ns2="http://security.jaxws.cxf.examples.
            fuses.jboss.org/"><return>Hello John Doe</return>
           </ns2:sayHiResponse>
          </soap:Body>
        </soap:Envelope>

**Note:** If you use Safari, right click the window and select **Show Source**.
**Note:** The EnableCORSInterceptor specified for the jaxws:endpoint is only for the CORS http header check of modern browser when use client.html to do the test.
      You can use the Java client instead to test your web service (see below).


### To run the test:

In this cxf-jaxws quistart, we also provide an integration test which can perform a few HTTP requests to test our web services. We
created a Maven `test` profile to allow us to run tests code with a simple Maven command after having deployed the bundle to Fuse:

1. Change to the `quickstarts/secure-soap` directory.
2. Run the following command:

        mvn -Ptest

The test uses a client proxy for the Web service to invoke the remote method - in reality,
a SOAP message will be sent to the server and the response SOAP message will be received and handled.  You will see this output from the remote method:

        Apr 4, 2012 7:48:13 AM org.apache.cxf.service.factory.ReflectionServiceFactoryBean buildServiceFromClass
        INFO: Creating Service {http://security.jaxws.cxf.examples.fuse.jboss.org/}HelloWorldService from class org.jboss.fuse.examples.cxf.jaxws.security.HelloWorld
        Hello World


### Managing the user credentials

You can define additional users in the JAAS realm in two ways:

1. By editing the `etc/users.properties` file, adding a line for every user your want to add (syntax: `user = password, roles`).

            myuser = mysecretpassword

2. Using the jaas: commands in the JBoss Fuse console:

            jaas:manage --realm karaf
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
--------------------

To stop and undeploy the bundle in Fuse:

1. Enter `osgi:list` command to retrieve your bundle id
2. To stop and uninstall the bundle enter

        osgi:uninstall <id>

# WS-SECURITY EXAMPLE

## Overview
This example demonstrates how to create a Web service with Apache CXF using WS-Security and Blueprint configuration, and expose it through the OSGi HTTP Service.


## What You Will Learn
In studying this example you will learn:

* how to configure JAX-WS Web services by using the blueprint configuration file.
* how to configure WS-Security on a CXF JAX-WS Web service in Blueprint
* how to use standard Java Web Service annotations to define a Web service interface
* how to use standard Java Web Service annotations when implementing a Web service in Java
* how to use use an HTTP URL to invoke a remote Web service

## Prerequisites
Before building and running this example you need:

* Maven 3.0.3 or higher
* JDK 1.6 or 1.7
* JBoss Fuse 6

## Files in the Example
* `pom.xml` - the Maven POM file for building the example
* `client.html` - a Web client that can be used to test the Web service from your browser
* `src/main/java/org.jboss.fuse.examples/cxf/jaxws/security/HelloWorld.java` - a Java interface that defines the Web service
* `src/main/java/org.jboss.fuse.examples/cxf/jaxws/security/HelloWorldImpl.java` - a Java class that implements the Web service
* `src/main/java/org.jboss.fuse.examples/cxf/jaxws/security/client/Client.java` - a Java class implementing a client that connects to the Web service using an HTTP URL
* `src/main/java/org.jboss.fuse.examples/cxf/jaxws/security/client/ClientPasswordCallback.java` - a Java class implementing authentication callback by checking the identifier and password
* `src/main/java/org.jboss.fuse.examples/cxf/jaxws/security/client/CustomSecurityInterceptor.java` - a Java class which set the security properties for the client 
* `src/main/resources/OSGI-INF/blueprint/blueprint.xml` - the OSGI Blueprint file that defines the services

## Building the Example
To build the example:

1. Change your working directory to the `examples/secure-soap` directory.
2. Run `mvn clean install` to build the example.


## Running the Example
To run the example:

1. Start JBoss Fuse 6 by running `bin/fuse` (on Linux) or `bin\fuse.bat` (on Windows).
2. In the JBoss Fuse console, enter the following command:
        osgi:install -s fab:mvn:org.jboss.fuse.examples/secure-soap/${project.version}
3. Verify etc/users.properties from the JBoss Fuse installation contains the following 'admin' user configured:
   admin=admin

There are several ways you can interact with the running Web services:
* browse the Web service metadata
* access the service in a Web browser
* use a Java client

### Browsing Web service metadata

A full listing of all CXF Web services is available at

    http://localhost:8181/cxf

After you deployed this example, you will see the `HelloWorldSecurity` service appear in the `Available SOAP Services` section, together with a list of operations for the endpoint and some additional information like the endpoint's address and a link to the WSDL file for the Web service:

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


### To run a Java client:

In this example project, we also developed a Java client which can perform a few HTTP requests to test our Web services. We
configured the exec-java-plugin in Maven to allow us to run the Java client code with a simple Maven command:

1. Change to the `<esb_home>/examples/secure-soap` directory.
2. Run the following command:

        mvn compile exec:java

The client uses a client proxy for the Web service to invoke the remote method - in reality, a SOAP message will be sent to the server and the response SOAP message will be received and handled.  You will see this output from the remote method:

        Apr 4, 2012 7:48:13 AM org.apache.cxf.service.factory.ReflectionServiceFactoryBean buildServiceFromClass
        INFO: Creating Service {http://security.jaxws.cxf.examples.fuse.jboss.org/}HelloWorldService from class org.jboss.fuse.examples.cxf.jaxws.security.HelloWorld
        Hello ffang


## Additional configuration options

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


## More information
For more information see:

* https://access.redhat.com/knowledge/docs/JBoss_Fuse/ for more information about using JBoss Fuse

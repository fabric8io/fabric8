# CXF JAXWS WS-SECURITY EXAMPLE

## Overview
This example demonstrates how to create a web service with CXF using WS-Security and Blueprint configuration, and expose it through the OSGi HTTP Service.


## What You Will Learn
In studying this example you will learn:

* how to configure JAX-WS web services by using the blueprint configuration file.
* how to configure WS-Security on a CXF JAX-WS webservice in Blueprint
* how to use standard Java Web Service annotations to define a web service interface
* how to use standard Java Web Service annotations when implementing a web service in Java
* how to use CXF's JaxWsProxyFactoryBean to create a client side proxy to invoke a remote web service

## Prerequisites
Before building and running this example you need:

* Maven 3.0.3 or higher
* JDK 1.6
* Fuse ESB Enterprise 7


## Building the Example
To build the example:

1. Change your working directory to the `examples/secure-soap` directory
2. Run `mvn clean install` to build the example


## Running the Example
To run the example:

1. Start Fuse ESB Enterprise 7 by running `bin/fuseesb` (on Linux) or `bin\fuseesb.bat` (on Windows)
2. In the Fuse ESB console, enter the following command:
        osgi:install -s fab:mvn:org.fusesource.examples/secure-soap/${project.version}

There are several ways you can interact with the running web services: you can browse the web service metadata,
but you can also invoke the web services in a few different ways.


### Browsing web service metadata

A full listing of all CXF web services is available at

    http://localhost:8181/cxf

After you deployed this example, you will see the 'HelloWorldSecurity' service appear in the 'Available SOAP Services' section,
together with a list of operations for the endpoint and some additional information like the endpoint's address and a link
to the WSDL file for the web service:

    http://localhost:8181/cxf/HelloWorldSecurity?wsdl


### To run a Web client:

1. Open the client.html, which is located in the same directory as this README file, in your favorite browser.
2. Click the Send button to send a request.

   Once the request has been successfully sent, a response similar to the following should appear in the right-hand panel of the web page:

         STATUS: 200

         <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
           <soap:Body><ns2:sayHiResponse xmlns:ns2="http://security.jaxws.cxf.examples.
            fusesource.org/"><return>Hello John Doe</return>
           </ns2:sayHiResponse>
          </soap:Body>
        </soap:Envelope>

Note: If you use Safari, right click the window and select 'Show Source'.
Note: If you get Status: 0 in the right-hand panel instead, your browser no longer supports a cross-domain HTTP request from JavaScript
      You can use the Java client instead to test your web service (see below).


### To run a Java client:

In this cxf-jaxws example project, we also developed a Java client which can perform a few HTTP requests to test our web services. We
configured the exec-java-plugin in Maven to allow us to run the Java client code with a simple Maven command:

1. Change to the <esb_home>/examples/secure-soap directory.
2. Run the following command:

        mvn compile exec:java

The client uses a client proxy for the webservice to invoke the remote method - in reality, a SOAP message will be sent to the server
and the response SOAP message will be received and handled.  You will see this output from the remote method:

        Apr 4, 2012 7:48:13 AM org.apache.cxf.service.factory.ReflectionServiceFactoryBean buildServiceFromClass
        INFO: Creating Service {http://security.jaxws.cxf.examples.fusesource.org/}HelloWorldService from class org.fusesource.examples.cxf.jaxws.security.HelloWorld
        Hello ffang


## Additional configuration options


### Managing the user credentials

You can define additional users in the JAAS realm in two ways:

1. By editing the etc/users.properties file, adding a line for every user your want to add (syntax: user = password, roles)

            myuser = mysecretpassword

2. Using the jaas: commands in the Fuse ESB Enterprise console:

            jaas:manage --realm karaf
            jaas:useradd myuser mysecretpassword
            jaas:update


### Changing /cxf servlet alias

By default CXF Servlet is assigned a '/cxf' alias. You can change it in a couple of ways

1. Add org.apache.cxf.osgi.cfg to the /etc directory and set the 'org.apache.cxf.servlet.context' property, for example:

        org.apache.cxf.servlet.context=/custom

2. Use shell config commands, for example:

        config:edit org.apache.cxf.osgi
        config:propset org.apache.cxf.servlet.context /custom
        config:update


## More information
For more information see:

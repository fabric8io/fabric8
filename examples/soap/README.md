# SOAP EXAMPLE

## Overview
This example demonstrates how to create a SOAP Web service with Apache CXF and expose it through the OSGi HTTP Service.

## What You Will Learn
In studying this example you will learn:

* how to configure JAX-WS Web services by using the blueprint configuration file
* how to configure additional CXF features like logging
* how to use standard Java Web Service annotations to define a Web service interface
* how to use standard Java Web Service annotations when implementing a Web service in Java
* how to use CXF's `JaxWsProxyFactoryBean` to create a client side proxy to invoke a remote Web service

## Prerequisites
Before building and running this example you need:

* Maven 3.0.3 or higher
* JDK 1.6 or 1.7
* JBoss Fuse 6

## Files in the Example
* `pom.xml` - the Maven POM file for building the example
* `client.html` - a Web client that can be used to test the Web service from your browser
* `src/main/java/org/fusesource/examples/cxf/jaxws/HelloWorld.java` - a Java interface that defines the Web service
* `src/main/java/org/fusesource/examples/cxf/jaxws/HelloWorldImpl.java` - a Java class that implements the Web service
* `src/main/java/org/fusesource/examples/cxf/jaxws/client/Client.java` - a Java class implementing a client that uses `JaxWsProxyFactoryBean` to call the Web service
* `src/main/resources/OSGI-INF/blueprint/blueprint.xml` - the OSGI Blueprint file that defines the services

## Building the Example
To build the example:

1. Change your working directory to the `examples/soap` directory
2. Run `mvn clean install` to build the example


## Running the Example
To run the example:

1. Start JBoss Fuse 6 by running `bin/fuseesb` (on Linux) or `bin\fuseesb.bat` (on Windows)
2. In the JBoss Fuse console, enter the following command:
        osgi:install -s fab:mvn:org.fusesource.examples/soap/${project.version}

There are several ways you can interact with the running web services: you can browse the web service metadata,
but you can also invoke the web services in a few different ways.


### Browsing web service metadata

A full listing of all CXF web services is available at

    http://localhost:8181/cxf

After you deployed this example, you will see the 'HelloWorld' service appear in the 'Available SOAP Services' section,
together with a list of operations for the endpoint and some additional information like the endpoint's address and a link
to the WSDL file for the web service:

    http://localhost:8181/cxf/HelloWorld?wsdl

You can also use "cxf:list-endpoints" to check the state of all CXF web services like this 

    JBossFuse:karaf@root> cxf:list-endpoints
    
    Name                      State      Address                                                      BusID                                   
    [HelloWorldImplPort     ] [Started ] [http://localhost:8181/cxf/HelloWorld                   ] [org.fusesource.examples.soap-cxf2040055609]
    

### To run a Web client:

1. Open the client.html, which is located in the same directory as this README file, in your favorite browser.
2. Click the Send button to send a request.

   Once the request has been successfully sent, a response similar to the following should appear in the right-hand panel of the web page:

         STATUS: 200

         <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
           <soap:Body><ns2:sayHiResponse xmlns:ns2="http://jaxws.cxf.examples.
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

1. Change to the <esb_home>/examples/soap directory.
2. Run the following command:

        mvn compile exec:java

The client sends the contents of the request.xml sample SOAP request file to the server and afterwards display the response message:

        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
          <soap:Body>
            <ns2:sayHiResponse xmlns:ns2="http://jaxws.cxf.examples.fusesource.org/">
              <return>Hello John Doe</return>
            </ns2:sayHiResponse>
          </soap:Body>
        </soap:Envelope>


## Additional configuration options

### Changing /cxf servlet alias

By default CXF Servlet is assigned a '/cxf' alias. You can change it in a couple of ways

1. Add org.apache.cxf.osgi.cfg to the /etc directory and set the 'org.apache.cxf.servlet.context' property, for example:

        org.apache.cxf.servlet.context=/custom
   
   In this way, JBoss Fuse will load the cfg when the CXF Servlet is reloaded, you can restart the CXF bundle to load the change.

2. Use shell config commands, for example:

        config:edit org.apache.cxf.osgi
        config:propset org.apache.cxf.servlet.context /custom
        config:update

    JBoss Fuse will create org.apache.cxf.osgi.cfg file in the /etc directory and and set the entry as we did in the first way after the commands are run, you need to restart the CXF bundle to load the change.


## More information
For more information see:

* http://fusesource.com/documentation/fuse-esb-enterprise-documentation for more information about using JBoss Fuse

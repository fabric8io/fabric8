soap: demonstrates a SOAP web service with Apache CXF
==========================

What is it?
-----------
This quick start demonstrates how to create a SOAP Web service with Apache CXF and expose it through the OSGi HTTP Service.

In studying this quick start you will learn:

* how to configure JAX-WS Web services by using the blueprint configuration file
* how to configure additional CXF features like logging
* how to use standard Java Web Service annotations to define a Web service interface
* how to use standard Java Web Service annotations when implementing a Web service in Java
* how to use CXF's `JaxWsProxyFactoryBean` to create a client side proxy to invoke a remote Web service

For more information see:

* http://fabric8.io/#/site/book/doc/index.md for more information about using Fabric8

System requirements
-------------------
Before building and running this quick start you need:

* Maven 3.0.4 or higher
* JDK 1.6 or 1.7
* Fabric8


Build and Deploy the Quickstart
-------------------------------

To build the quick start:

1.Change your working directory to `soap` directory.
* Run `mvn clean install` to build the quickstart.
* Start Fabric8 by running bin/fabric8 (on Linux) or bin\fabric8.bat (on Windows).
* In the Fabric8 console, enter the following command:

        osgi:install -s mvn:io.fabric8.quickstarts.fabric/soap/${project.version}

* Fabric8 should give you on id when the bundle is deployed
* You can check that everything is ok by issue the command:

        osgi:list
   your bundle should be present at the end of the list


Use the bundle
--------------

There are several ways you can interact with the running web services: you can browse the web service metadata,
but you can also invoke the web services in a few different ways.


### Browsing web service metadata

A full listing of all CXF web services is available at

    http://localhost:8181/cxf

After you deployed this quick start, you will see the 'HelloWorld' service appear in the 'Available SOAP Services' section,
together with a list of operations for the endpoint and some additional information like the endpoint's address and a link
to the WSDL file for the web service:

    http://localhost:8181/cxf/HelloWorld?wsdl

You can also use "cxf:list-endpoints" in Fabric8 to check the state of all CXF web services like this 

    Fabric8:karaf@root> cxf:list-endpoints
    
    Name                      State      Address                                                      BusID                                   
    [HelloWorldImplPort     ] [Started ] [http://localhost:8181/cxf/HelloWorld                   ] [io.fabric8.fabric.examples.soap-cxf2040055609]
    

### To run a Web client:

You can use an external tool such as SoapUI to test web services.


### To run the test:

In this cxf-jaxws quistart, we also provide an integration test which can perform a few HTTP requests to test our web services. We
created a Maven `test` profile to allow us to run tests code with a simple Maven command after having deployed the bundle to Fabric8:

1. Change to the `soap` directory.
2. Run the following command:

        mvn -Ptest

    The test sends the contents of the request.xml sample SOAP request file to the server and afterwards display the response
    message:

        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
          <soap:Body>
            <ns2:sayHiResponse xmlns:ns2="http://soap.fabric.quickstarts.fabric8.io/">
              <return>Hello John Doe</return>
            </ns2:sayHiResponse>
          </soap:Body>
        </soap:Envelope>


### Changing /cxf servlet alias

By default CXF Servlet is assigned a '/cxf' alias. You can change it in a couple of ways

1. Add org.apache.cxf.osgi.cfg to the /etc directory and set the 'org.apache.cxf.servlet.context' property, for example:

        org.apache.cxf.servlet.context=/custom
   
   In this way, Fabric8 will load the cfg when the CXF Servlet is reloaded, you can restart the CXF bundle to load the change.

2. Use shell config commands, for example:

        config:edit org.apache.cxf.osgi
        config:propset org.apache.cxf.servlet.context /custom
        config:update

    Fabric8 will create org.apache.cxf.osgi.cfg file in the /etc directory and and set the entry as we did in the first way after the commands are run, you need to restart the CXF bundle to load the change.
    
Undeploy the Bundle
-------------------

To stop and undeploy the bundle in Fabric8:

1. Enter `osgi:list` command to retrieve your bundle id
2. To stop and uninstall the bundle enter

        osgi:uninstall <id>


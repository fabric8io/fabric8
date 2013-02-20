# RESTful web services with CXF

## Overview
This example demonstrates how to create a RESTful (JAX-RS) web service using CXF and expose it with the OSGi HTTP Service.

## What You Will Learn
In studying this example you will learn:

* how to configure the JAX-RS web services by using the blueprint configuration file.
* how to use JAX-RS annotations to map methods and classes to URIs
* how to use JAXB annotations to define beans and output XML responses
* how to use the JAX-RS API to create HTTP responses

## Prerequisites
Before building and running this example you need:

* Maven 3.0.3 or higher
* JDK 1.6 or 1.7
* JBoss Fuse 6

## Files in the Example
* `pom.xml` - the Maven POM file for building the example
* `src/main/java/org/fusesource/examples/cxf/jaxrs/Customer.java` - a Java class defining the JAXB representation of the Customer element processed by the example
* `src/main/java/org/fusesource/examples/cxf/jaxrs/CustomerService.java` - a Java class implementing the service that handles customer requests using JAXRS
* `src/main/java/org/fusesource/examples/cxf/jaxrs/Order.java` - a Java class defining the JAXB representation of the Order element processed by the example. It also defines a JAXRS sub-resource that processes orders.
* `src/main/java/org/fusesource/examples/cxf/jaxrs/Prooduct.java` - a Java class defining the JAXB representation of the Product element used in the orders
* `src/main/java/org/fusesource/examples/cxf/jaxrs/client/Client.java` - a Java class implementing an HTTP client that can be used to test the service
* `src/main/resources/org/fusesource/examples/cxf/jaxrs/client/*.xml` - data files used by the client to test the service
* `src/main/resources/OSGI-INF/blueprint/blueprint.xml` - the OSGI Blueprint file that defines the services

## Building the Example
To build the example:

1. Change your working directory to the `examples/rest` directory.
2. Run `mvn clean install` to build the example.

## Running the Example
To run the example:

1. Start JBoss Fuse 6 by running `bin/fuse` (on Linux) or `bin\fuse.bat` (on Windows).
2. In the JBoss Fuse console, enter the following command:
        osgi:install -s fab:mvn:org.fusesource.examples/rest/${project.version}

There are several ways you can interact with the running RESTful Web services:
* browse the Web service metadata
* access the service in a Web browser
* use a Java client
* use a command-line utility

### Browsing Web service metadata

A full listing of all CXF web services is available at

    http://localhost:8181/cxf

After you deployed this example, you will see the following endpoint address appear in the 'Available RESTful services' section:

    http://localhost:8181/cxf/crm

Just below it, you'll find a link to the WADL describing all the root resources:

    http://localhost:8181/cxf/crm?_wadl

You can also look at the more specific WADL, the only that only lists information about 'customerservice' itself:

	http://localhost:8181/cxf/crm/customerservice?_wadl&_type=xml

### Access services using a web browser

You can use any browser to perform a HTTP GET.  This allows you to very easily test a few of the RESTful services we defined:

Use this URL to display the XML representation for customer 123:

    http://localhost:8181/cxf/crm/customerservice/customers/123

You can also access the XML representation for order 223 ...

    http://localhost:8181/cxf/crm/customerservice/orders/223

... or the XML representation of product 323 in order 223 with

    http://localhost:8181/cxf/crm/customerservice/orders/223/products/323

**Note:** if you use Safari, you will only see the text elements but not the XML tags - you can view the entire document with 'View Source'

### To run a Java client:

In this example project, we also provide a Java client which can perform a few HTTP requests to test our Web services. We
configured the exec-java-plugin in Maven to allow us to run the Java client code with a simple Maven command:

1. Change to the `<esb_home>/examples/rest` directory.
2. Run the following command:

        mvn compile exec:java
        
The client makes a sequence of RESTful invocations and displays the results.

### To run a command-line utility:

You can use a command-line utility, such as cURL or wget, to perform the HTTP requests.  We have provided a few files with sample XML representations in `src/main/resources/org/fusesource/examples/cxf/jaxrs/client`, so we will use those for testing our services.

1. Open a command prompt and change directory to `<esb_home>/examples/rest`.
2. Run the following curl commands (curl commands may not be available on all platforms):
    
    * Create a customer
 
            curl -X POST -T src/main/resources/org/fusesource/examples/cxf/jaxrs/client/add_customer.xml -H "Content-Type: text/xml" http://localhost:8181/cxf/crm/customerservice/customers
  
    * Retrieve the customer instance with id 123
    
            curl http://localhost:8181/cxf/crm/customerservice/customers/123

    * Update the customer instance with id 123
  
            curl -X PUT -T src/main/resources/org/fusesource/examples/cxf/jaxrs/client/update_customer.xml -H "Content-Type: text/xml" http://localhost:8181/cxf/crm/customerservice/customers

    * Delete the customer instance with id 123
  
             curl -X DELETE http://localhost:8181/cxf/crm/customerservice/customers/123

## Additional configuration options

### Changing /cxf servlet alias

By default CXF Servlet is assigned a '/cxf' alias. You can change it in a couple of ways

1. Add `org.apache.cxf.osgi.cfg` to the `/etc` directory and set the `org.apache.cxf.servlet.context` property, for example:

        org.apache.cxf.servlet.context=/custom

2. Use shell config commands, for example:

        config:edit org.apache.cxf.osgi
        config:propset org.apache.cxf.servlet.context /custom
        config:update

## More information

For more information see:

* http://fusesource.com/documentation/fuse-esb-enterprise-documentation for more information about using JBoss Fuse
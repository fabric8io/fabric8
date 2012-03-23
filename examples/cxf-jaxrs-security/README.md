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
* JDK 1.6
* Fuse ESB Enterprise 7

## Building the Example
To build the example:

1. Change your working directory to the `examples/cxf-jaxrs` directory
1. Run `mvn clean install` to build the example

## Running the Example
To run the example:

1. Start Fuse ESB Enterprise 7 by running `bin/fuseesb` (on Linux) or `bin\fuseesb.bat` (on Windows)
1. In the Fuse ESB console, enter the following command:
        osgi:install -s mvn:org.fusesource.examples/cxf-jaxrs-security/${project.version}

There are several ways you can interact with the running RESTful web services: you can browse the web service metadata,
but you can also invoke the web services in a few different ways.

### Browsing web service metadata

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

Use this URL to access the XML representation for customer 123:

    http://localhost:8181/cxf/crm/customerservice/customers/123

Because we need to pass along credentials to actually access the service in this security-enabled example, we will get a fault
message indicating a security exception at this time.

Note: if you use Safari, you will only see the text elements but not the XML tags - you can view the entire document with 'View Source'

### To run a Java client:

In this cxf-jaxrs example project, we also developed a Java client which can perform a few HTTP requests to test our web services. We
configured the exec-java-plugin in Maven to allow us to run the Java client code with a simple Maven command:

1. Change to the <esb_home>/examples/cxf-jaxrsdirectory.
2. Run the following command:

        mvn compile exec:java
        
The client makes a sequence of RESTful invocations and displays the results.

### To run a command-line utility:

You can use a command-line utility, such as cURL or wget, to perform the HTTP requests.  We have provided a few files with sample
XML representations in src/main/resources/org/fusesource/examples/cxf/jaxrs/client, so we will use those for testing our services.

1. Open a command prompt and change directory to <esb_home>/examples/cxf-jaxrs.
2. Run the following curl commands:
    
    * Create a customer
 
            curl --basic -u smx:smx -X POST -T src/main/resources/org/fusesource/examples/cxf/jaxrs/client/add_customer.xml -H "Content-Type: text/xml" http://localhost:8181/cxf/crm/customerservice/customers
  
    * Retrieve the customer instance with id 123
    
            curl --basic -u smx:smx http://localhost:8181/cxf/crm/customerservice/customers/123

    * Update the customer instance with id 123
  
            curl --basic -u smx:smx -X PUT -T src/main/resources/org/fusesource/examples/cxf/jaxrs/client/update_customer.xml -H "Content-Type: text/xml" http://localhost:8181/cxf/crm/customerservice/customers

    * Delete the customer instance with id 123
  
             curl --basic -u smx:smx -X DELETE http://localhost:8181/cxf/crm/customerservice/customers/123

## Additional configuration options

### Managing the user credentials

You can define additional users in the JAAS realm in two ways:

 1. By editing the etc/users.properties file, adding a line for every user your want to add (syntax: user = password, roles)

             myuser = mysecretpassword

 1. Using the jaas: console commands

             jaas:manage --realm karaf
             jaas:adduser myuser mysecretpassword
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




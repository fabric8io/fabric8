# RESTful web services with CXF

## Overview
This example demonstrates how to create a RESTful (JAX-RS) Web service using CXF and expose it with the OSGi HTTP Service.

## What You Will Learn
In studying this example you will learn:

* how to configure the JAX-RS Web services by using the blueprint configuration file.
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
* `src/main/java/org/fusesource/examples/cxf/jaxrs/security/Customer.java` - a Java class defining the JAXB representation of the Customer element processed by the example
* `src/main/java/org/fusesource/examples/cxf/jaxrs/security/CustomerService.java` - a Java class implementing the service that handles customer requests using JAXRS
* `src/main/java/org/fusesource/examples/cxf/jaxrs/security/Order.java` - a Java class defining the JAXB representation of the Order element processed by the example. It also defines a JAXRS sub-resource that processes orders.
* `src/main/java/org/fusesource/examples/cxf/jaxrs/security/Prooduct.java` - a Java class defining the JAXB representation of the Product element used in the orders
* `src/main/java/org/fusesource/examples/cxf/jaxrs/security/client/Client.java` - a Java class implementing an HTTP client that can be used to test the service
* `src/main/resources/org/fusesource/examples/cxf/jaxrs/security/client/*.xml` - data files used by the client to test the service
* `src/main/resources/OSGI-INF/blueprint/blueprint.xml` - the OSGI Blueprint file that defines the services

## Building the Example
To build the example:

1. Change your working directory to the `examples/secure-rest` directory.
2. Run `mvn clean install` to build the example.

## Running the Example
To run the example:

1. Start JBoss Fuse 6 by running `bin/fuseesb` (on Linux) or `bin\fuseesb.bat` (on Windows).
2. In the JBoss Fuse console, enter the following command:
        osgi:install -s fab:mvn:org.fusesource.examples/secure-rest/${project.version}
3. Verify etc/users.properties from the JBoss Fuse installation contains the following 'admin' user configured:
admin=admin
4. edit etc/jetty.xml and comment out
    <Call name="addBean">
        <Arg>
            <New class="org.eclipse.jetty.plus.jaas.JAASLoginService">
                <Set name="name">karaf</Set>
                <Set name="loginModuleName">karaf</Set>
                <Set name="roleClassNames">
                    <Array type="java.lang.String">
                        <Item>org.apache.karaf.jaas.boot.principal.RolePrincipal</Item>
                    </Array>
                </Set>
            </New>
        </Arg>
    </Call>
as this example use cxf JAASLoginInterceptor with KARAF jaas realm to do authentication, shouldn't have 
jetty JAASLoginService enabled to do the authentication.

There are several ways you can interact with the running RESTful Web services:
* browse the Web service metadata
* access the service in a Web browser
* use a Java client
* use a command-line utility


### Browsing Web service metadata

A full listing of all CXF Web services is available at

    http://localhost:8181/cxf

After you deployed this example, you will see the following endpoint address appear in the 'Available RESTful services' section:

    http://localhost:8181/cxf/securecrm

Just below it, you'll find a link to the WADL describing all the root resources:

    http://localhost:8181/cxf/securecrm?_wadl

You can also look at the more specific WADL, the only that only lists information about 'customerservice' itself:

	http://localhost:8181/cxf/securecrm/customerservice?_wadl&_type=xml

### Access services using a Web browser

You can use any browser to perform a HTTP GET.  This allows you to very easily test a few of the RESTful services we defined:

Use this URL to access the XML representation for customer 123:

    http://localhost:8181/cxf/securecrm/customerservice/customers/123

Because we need to pass along credentials to actually access the service in this security-enabled example, we will get a fault
message indicating a security exception at this time.

**Note:** if you use Safari, you will only see the text elements but not the XML tags - you can view the entire document with 'View Source'

### To run a Java client:

In this example, we also developed a Java client which can perform a few HTTP requests to test our web services. We
configured the exec-java-plugin in Maven to allow us to run the Java client code with a simple Maven command:

1. Change to the `<esb_home>/examples/secure-rest` directory.
2. Run the following command:

        mvn compile exec:java
        
The client makes a sequence of RESTful invocations and displays the results.

### To run a command-line utility:

You can use a command-line utility, such as cURL or wget, to perform the HTTP requests.  We have provided a few files with sample XML representations in `src/main/resources/org/fusesource/examples/cxf/jaxrs/security/client`, so we will use those for testing our services.

1. Open a command prompt and change directory to `<esb_home>/examples/cxf-jaxrs-security`.
2. Run the following curl commands (curl commands may not be available on all platforms):
    
    * Create a customer
 
            curl --basic -u admin:admin -X POST -T src/main/resources/org/fusesource/examples/cxf/jaxrs/security/client/add_customer.xml -H "Content-Type: text/xml" http://localhost:8181/cxf/securecrm/customerservice/customers
  
    * Retrieve the customer instance with id 123
    
            curl --basic -u admin:admin http://localhost:8181/cxf/securecrm/customerservice/customers/123

    * Update the customer instance with id 123
  
            curl --basic -u admin:admin -X PUT -T src/main/resources/org/fusesource/examples/cxf/jaxrs/security/client/update_customer.xml -H "Content-Type: text/xml" http://localhost:8181/cxf/securecrm/customerservice/customers

    * Delete the customer instance with id 123
  
             curl --basic -u admin:admin -X DELETE http://localhost:8181/cxf/securecrm/customerservice/customers/123

## Additional configuration options

### Managing the user credentials

You can define additional users in the JAAS realm in two ways:

 1. By editing the `etc/users.properties` file, adding a line for every user your want to add (syntax: `user = password, roles`)

             myuser = mysecretpassword

 2. Using the `jaas:` commands in the JBoss Fuse console:

             jaas:manage --realm karaf
             jaas:useradd myuser mysecretpassword
             jaas:update

### Changing /cxf servlet alias

By default CXF Servlet is assigned a '/cxf' alias. You can change it in a couple of ways

1. Add `org.apache.cxf.osgi.cfg` to the `/etc` directory and set the 'org.apache.cxf.servlet.context' property, for example:

        org.apache.cxf.servlet.context=/custom

2. Use shell config commands, for example:

        config:edit org.apache.cxf.osgi
     
        config:propset org.apache.cxf.servlet.context /custom
     
        config:update

## More information
For more information see:

* http://fusesource.com/documentation/fuse-esb-enterprise-documentation for more information about using JBoss Fuse

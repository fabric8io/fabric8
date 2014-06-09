rest: demonstrates RESTful web services with CXF
===============================================

What is it?
-----------
This quick start demonstrates how to create a RESTful (JAX-RS) web service using CXF and expose it with the OSGi HTTP Service.

In studying this quick start you will learn:

* how to configure the JAX-RS web services by using the blueprint configuration file.
* how to use JAX-RS annotations to map methods and classes to URIs
* how to use JAXB annotations to define beans and output XML responses
* how to use the JAX-RS API to create HTTP responses

For more information see:

* http://fabric8.io/gitbook/ for more information about using Fabric8


System requirements
-------------------
Before building and running this quick start you need:

* Maven 3.0.4 or higher
* JDK 1.6 or 1.7
* Fabric8

Build and Deploy the Quickstart
-------------------------------

1. Change your working directory to `rest` directory.
* Run `mvn clean install` to build the quickstart.
* Start Fabric8 by running bin/fabric8 (on Linux) or bin\fabric8.bat (on Windows).
* When its started up and you can open the fabric8 console, type this into your maven shell

    mvn fabric8:deploy

* Now if you navigate to the Profiles page in the web console you should see the newly created profile.
* Click on the Profile page and click the "New" button on the top right to create a new container.


Use the WAR
-----------

### Access services using a web browser

You can use any browser to perform a HTTP GET.  This allows you to very easily test a few of the RESTful services we defined:

Use this URL to display the XML representation for customer 123:

    http://localhost:8080/rest-web/cxf/customerservice/customers/123

You can also access the XML representation for order 223 ...

    http://localhost:8080/rest-web/cxf/customerservice/orders/223

... or the XML representation of product 323 in order 223 with

    http://localhost:8080/rest-web/cxf/customerservice/orders/223/products/323

**Note:** if you use Safari, you will only see the text elements but not the XML tags - you can view the entire document with 'View Source'

### To run the tests:

In this quick start project, we also provide integration tests which perform a few HTTP requests to test our Web services. We
created a Maven `test` profile to allow us to run tests code with a simple Maven command after having deployed the bundle to Fabric8:

1. Change to the `rest` directory.
2. Run the following command:

        mvn -Ptest
        
The tests in `src/test/java/io.fabric8.quickstarts.fabric.rest/CrmTest`  make a sequence of RESTful invocations and displays the results.

### To run a command-line utility:

You can use a command-line utility, such as cURL or wget, to perform the HTTP requests.  We have provided a few files with sample XML representations in `src/test/resources`, so we will use those for testing our services.

1. Open a command prompt and change directory to `rest`.
2. Run the following curl commands (curl commands may not be available on all platforms):
    
    * Create a customer
 
            curl -X POST -T src/test/resources/add_customer.xml -H "Content-Type: text/xml" http://localhost:8080/rest-web/cxf/customerservice/customers
  
    * Retrieve the customer instance with id 123
    
            curl http://localhost:8080/rest-web/cxf/customerservice/customers/123

    * Update the customer instance with id 123
  
            curl -X PUT -T src/test/resources/update_customer.xml -H "Content-Type: text/xml" http://localhost:8080/rest-web/cxf/customerservice/customers

    * Delete the customer instance with id 123
  
             curl -X DELETE http://localhost:8080/rest-web/cxf/customerservice/customers/123

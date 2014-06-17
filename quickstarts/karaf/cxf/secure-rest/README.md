secure-rest: demonstrates secure RESTful web services with CXF
===============================================

## What is it?

This quickstart demonstrates how to create a secure RESTful (JAX-RS) web service using CXF and expose it with the OSGi HTTP Service.


## System requirements

Before building and running this quick start you need:

* Maven 3.0.4 or higher
* JDK 1.7
* Fabric8


## Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To build from the source code:

1. Change your working directory to `secure-rest` directory.
1. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `secure-rest` directory.
1. Run `mvn fabric8:deploy` to upload the quickstart to the fabric container.

If you run the `fabric:deploy` command for the first then, it will ask you for the username and password to login the fabric container.
And then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).


## How to run this example

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `quickstarts-secure.rest` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile quickstarts-secure.rest root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.
1. Log into the `mychild` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect mychild

1. View the container log using the `log:tail` command as follows:

        log:tail


### How to try this example

Login to the web console and click the APIs button on the Runtime plugin

    http://localhost:8181/hawtio/index.html#/fabric/api

This shows the REST service in the fabric.

You can try the REST service by clicking either the swagger or WADL in the APIs column. This takes you to a web page that lists the REST operations you can try.

For example click on GET customers/{id} and in the form enter `123` in the id field, and click the `Try it out!` button. You should get a XML response with custome details.


### To run a command-line utility:

You can use a command-line utility, such as cURL or wget, to perform the HTTP requests.  We have provided a few files with sample XML representations in `src/test/resources`, so we will use those for testing our services.

1. Open a command prompt and change directory to `rest`.
2. Run the following curl commands (curl commands may not be available on all platforms):
    
    * Create a customer

            curl --basic -u admin:admin -X POST -T src/test/resources/add_customer.xml -H "Content-Type: text/xml" http://localhost:8181/cxf/securecrm/customerservice/customers

    * Retrieve the customer instance with id 123

            curl --basic -u admin:admin http://localhost:8181/cxf/securecrm/customerservice/customers/123

    * Update the customer instance with id 123

            curl --basic -u admin:admin -X PUT -T src/test/resources/update_customer.xml -H "Content-Type: text/xml" http://localhost:8181/cxf/securecrm/customerservice/customers

    * Delete the customer instance with id 123

             curl --basic -u admin:admin -X DELETE http://localhost:8181/cxf/securecrm/customerservice/customers/123


## Undeploy this example

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
2. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild
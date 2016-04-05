secure-rest: Demonstrates a Secure REST Web service
======================================================
Author: Fuse Team  
Level: Beginner  
Technologies: Camel, CXF, REST  
Summary: This quickstart demonstrates how to create a secure RESTful (JAX-RS) web service using CXF and expose it through the OSGi HTTP Service.  
Target Product: Fuse  
Source: <https://github.com/jboss-fuse/quickstarts>  

### Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To try the example you do not need to build from source first. Although building from source allows you to modify the source code, and re-deploy the changes to fabric. See more details on the fabric8 website about the [developer workflow](http://fabric8.io/gitbook/developer.html).

To build from the source code:

1. Change your working directory to `quickstarts/cxf/secure-rest` directory.
1. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `quickstarts/cxf/secure-rest` directory.
1. Run `mvn fabric8:deploy` to upload the quickstart to the fabric container.

If you run the `fabric:deploy` command for the first then, it will ask you for the username and password to login the fabric container.
And then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).


## How to run this example

The following information is divded into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the command line shell

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `quickstarts-cxf-secure.rest` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile quickstarts-cxf-secure.rest root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.
1. Log into the `mychild` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect mychild

1. View the container log using the `log:tail` command as follows:

        log:tail

To exit the tail logger, press Ctrl-D. And to logout from the `mychild` container, then use the `exit` command, which returns back to the `root` container.

### Using the web console

You can deploy and run this example from the web console, as follows

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Login the web console
1. Click the Wiki button in the navigation bar
1. Select `quickstarts` --> `cxf` --> `secure.rest`
1. Click the `New` button in the top right corner
1. In the Create New Container page, enter `mychild` in the Container Name field, and click the *Create and start container* button


### How to try this example

Login to the web console and click the APIs button on the Runtime plugin, to show the REST services in the fabric

You can try the REST service by clicking either the swagger or WADL in the APIs column. This takes you to a web page that lists the REST operations you can try.

For example click on GET customers/{id} and in the form enter `123` in the id field, and click the `Try it out!` button. You should get a XML response with custome details.


### To run a command-line utility:

You can use a command-line utility, such as cURL or wget, to perform the HTTP requests.  We have provided a few files with sample XML representations in `src/test/resources`, so we will use those for testing our services.

Notice: As fabric8 assigns a free dynamic port to Karaf, the port number may vary on your system.

1. Open a command prompt and change directory to `rest`.
2. Run the following curl commands (curl commands may not be available on all platforms):
    
    * Create a customer

            curl --basic -u admin:admin -X POST -T src/test/resources/add_customer.xml -H "Content-Type: text/xml" http://localhost:8182/cxf/securecrm/customerservice/customers

    * Retrieve the customer instance with id 123

            curl --basic -u admin:admin http://localhost:8181/cxf/securecrm/customerservice/customers/123

    * Update the customer instance with id 123

            curl --basic -u admin:admin -X PUT -T src/test/resources/update_customer.xml -H "Content-Type: text/xml" http://localhost:8182/cxf/securecrm/customerservice/customers

    * Delete the customer instance with id 123

             curl --basic -u admin:admin -X DELETE http://localhost:8182/cxf/securecrm/customerservice/customers/123


## Undeploy this example

The following information is divded into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the command line shell

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
1. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild

### Using the web console

To stop and undeploy the example in fabric8:

1. In the web console, click the *Runtime* button in the navigation bar.
1. Select the `mychild` container in the *Containers* list, and click the *Stop* button in the top right corner


rest: demonstrates RESTful web services with CXF
===============================================

## What is it?

This quick start demonstrates how to create a RESTful (JAX-RS) web service using CXF and expose it in an Apache Tomcat container.


## System requirements

Before building and running this quick start you need:

* Maven 3.0.4 or higher
* JDK 1.7
* Fabric8


## Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To build from the source code:

1. Change your working directory to `rest-war` directory.
1. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `rest-war` directory.
1. Run `mvn fabric8:deploy` to upload the quickstart to the fabric container.

If you run the `fabric:deploy` command for the first then, it will ask you for the username and password to login the fabric container.
And then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).


## How to run this example

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `quickstarts-rest.war` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile quickstarts-rest.war root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.

### How to try this example

Login to the web console

    http://localhost:8181/hawtio/index.html

In the web console you should see a list of containers. Click the open button for the `mychild` container, which opens a new tab.

Click the Tomcat button, which lists all the WAR applications deployed. Click the url link for the `rest-war` application,
which opens a web page, that allows you to try this example. On the web page there is links to GET and UPDATE a customer etc.

### Access services using a web browser

You can use any browser to perform a HTTP GET.  This allows you to very easily test a few of the RESTful services we defined:

Notice: As fabric8 assigns a free dynamic port to Tomcat, the port number may vary on your system.

Use this URL to display the XML representation for customer 123:

    http://192.168.1.3:9004/rest-war/cxf/customerservice/customers/123

You can also access the XML representation for order 223 ...

    http://192.168.1.3:9004/rest-war/cxf/customerservice/orders/223

**Note:** if you use Safari, you will only see the text elements but not the XML tags - you can view the entire document with 'View Source'

### To run a command-line utility:

You can use a command-line utility, such as cURL or wget, to perform the HTTP requests.  We have provided a few files with sample XML representations in `src/test/resources`, so we will use those for testing our services.

1. Open a command prompt and change directory to `rest`.
2. Run the following curl commands (curl commands may not be available on all platforms):

    * Create a customer

            curl -X POST -T src/test/resources/add_customer.xml -H "Content-Type: text/xml" http://192.168.1.3:9004/rest-war/cxf/customerservice/customers

    * Retrieve the customer instance with id 123

            curl http://192.168.1.3:9004/rest-war/cxf/customerservice/customers/123

    * Update the customer instance with id 123

            curl -X PUT -T src/test/resources/update_customer.xml -H "Content-Type: text/xml" http://192.168.1.3:9004/rest-war/cxf/customerservice/customers

    * Delete the customer instance with id 123

             curl -X DELETE http://192.168.1.3:9004/rest-war/cxf/customerservice/customers/123


## Undeploy this example

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
2. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild


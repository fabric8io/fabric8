secure-soap: demonstrates a secure SOAP web service with Apache CXF
==========================

## What is it?

This quickstart demonstrates how to create a secure SOAP Web service with Apache CXF and expose it through the OSGi HTTP Service.


## System requirements

Before building and running this quick start you need:

* Maven 3.0.4 or higher
* JDK 1.7
* Fabric8


## Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To build from the source code:

1. Change your working directory to `secure-soap` directory.
1. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `secure-soap` directory.
1. Run `mvn fabric8:deploy` to upload the quickstart to the fabric container.

If you run the `fabric:deploy` command for the first then, it will ask you for the username and password to login the fabric container.
And then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).


## How to run this example

To build the quick start:

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `quickstarts-secure.soap` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile quickstarts-secure.soap root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.
1. Log into the `mychild` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect mychild

1. View the container log using the `log:tail` command as follows:

        log:tail


### How to try this example

Login to the web console and click the APIs button on the Runtime plugin

    http://localhost:8181/hawtio/index.html#/fabric/api

This shows the SOAP services in the fabric.

You can see details of the SOAP service by clicking the WSDL under the APIs column. 

The WSDL for the SOAP service is the `Location` url and append `?wsdl`


### To run a Web client:

You can use an external tool such as SoapUI to test web services.

When using SoapUI with WS Security, then configure the request properties as follows:

* Username = admin
* Password = admin
* Authentication Type = Global HTTP Settings
* WSS-Password Type = PasswordText


## Undeploy this example

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
2. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild

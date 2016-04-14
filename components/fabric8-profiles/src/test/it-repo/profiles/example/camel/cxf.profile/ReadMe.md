## Camel CXF Demo

This example demonstrates using Camel CXF as a SOAP web service.

The example is implemented using code-first development, where the web service is coded in the
java source file **io.fabric8.examples.camelcxf.Greeter**.

When the example runs, the web service is exposed at **http://localhost:9090/greeter/**.
You can also see the web service in the [API](#/fabric/api) browser in the web console.

Notice: As fabric8 assigns a free dynamic port to the container, the port number may vary on your system.
The port number is defined as range which you can specify in the `io.fabric8.examples.camel.cxf.properties` file.


## How to run this example

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `example-camel-cxf` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile example-camel-cxf root mychild

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

The WSDL for the SOAP service is the `Location` url and append `?wsdl`.


### To run a Web client:

You can use an external tool such as SoapUI to test web services.


## Undeploy this example

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
2. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild



